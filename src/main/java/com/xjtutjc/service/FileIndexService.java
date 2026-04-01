package com.xjtutjc.service;

import com.xjtutjc.model.EmbeddingModel;
import com.xjtutjc.service.entity.DocChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class FileIndexService {
    @Resource
    private ChunkService chunkService;
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private EmbeddingModel embeddingModel;
    public void indexSingleFile(String filePath) throws Exception {
        Path path = Paths.get(filePath).normalize();
        File file = path.toFile();

        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        log.info("开始索引文件: {}", path);

        // 1. 读取文件内容
        String content = Files.readString(path);
        log.info("读取文件: {}, 内容长度: {} 字符", path, content.length());

        // 2. 删除该文件的旧数据（如果存在）
        deleteExistingData(path.toString());

        // 3. 文档分片
        List<DocChunk> chunks = chunkService.chunkDocument(content, path.toString());
        log.info("文档分片完成: {} -> {} 个分片", filePath, chunks.size());

        // 4. 为每个分片生成向量并插入 向量数据库
        for (int i = 0; i < chunks.size(); i++) {
            DocChunk chunk = chunks.get(i);

            try {
                // 生成向量
                float[] floatEmbedding = embeddingModel.getFloatEmbedding(chunk.getContent());

                // 构建元数据（包含文件信息）
                Metadata metadata = buildMetadata(path.toString(), chunk, chunks.size());

                // 插入到 qdrant
                insertToQdrant(chunk.getContent(), floatEmbedding, metadata, chunk.getChunkIndex());

                log.info("✓ 分片 {}/{} 索引成功", i + 1, chunks.size());

            } catch (Exception e) {
                log.error("✗ 分片 {}/{} 索引失败", i + 1, chunks.size(), e);
                throw new RuntimeException("分片索引失败: " + e.getMessage(), e);
            }
        }

        log.info("文件索引完成: {}, 共 {} 个分片", filePath, chunks.size());
    }

    public void deleteExistingData(String filePath){
        try {
            // 使用统一的路径分隔符（正斜杠）用于qdrant存储，避免表达式解析错误
            // 将系统路径转换为统一格式
            Path path = Paths.get(filePath).normalize();
            String normalizedPath = path.toString().replace(File.separator, "/");
            Filter filter = new IsEqualTo("_source",normalizedPath);
            embeddingStore.removeAll(filter);
        }catch (Exception e){
            log.error("删除向量到 qdrant 失败", e);
            throw e;
        }

    }

    private Metadata buildMetadata(String filePath, DocChunk chunk, Integer totalChunks){
        Metadata metadata = new Metadata();

        // 标准化路径：使用统一的路径分隔符（正斜杠）用于存储，确保跨平台一致性
        Path path = Paths.get(filePath).normalize();
        String normalizedPath = path.toString().replace(File.separator, "/");

        // 文件信息
        Path fileName = path.getFileName();
        String fileNameStr = fileName != null ? fileName.toString() : "";
        String extension = "";
        int dotIndex = fileNameStr.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileNameStr.substring(dotIndex);
        }

        metadata.put("_source", normalizedPath);
        metadata.put("_extension", extension);
        metadata.put("_file_name", fileNameStr);

        // 分片信息
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("totalChunks", totalChunks);
        metadata.put("userName", fileNameStr);

        // 标题信息
        if (chunk.getTitle() != null && !chunk.getTitle().isEmpty()) {
            metadata.put("title", chunk.getTitle());
        }

        return metadata;
    }

    private void insertToQdrant(String content,float[] vector, Metadata metadata, int index){
        try {
            // 生成唯一 ID（使用 _source + 分片索引）
            String source = metadata.getString("_source");
            String id = UUID.nameUUIDFromBytes((source + "_" + index).getBytes()).toString();
            //注意，在写入时，我们传入的id会再经历一次uuid生成
            TextSegment textSegment = new TextSegment(content,metadata);
            Embedding embedding = new Embedding(vector);
            embeddingStore.addAll(Collections.singletonList(id),Collections.singletonList(embedding),Collections.singletonList(textSegment));
        }catch (Exception e){
            log.error("插入向量到 qdrant 失败", e);
            throw e;
        }
    }
}
