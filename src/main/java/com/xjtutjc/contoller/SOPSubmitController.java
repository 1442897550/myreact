package com.xjtutjc.contoller;

import com.xjtutjc.dto.request.SopSearchRequestDTO;
import com.xjtutjc.dto.request.SopSubmitRequestDTO;
import com.xjtutjc.dto.response.*;
import com.xjtutjc.model.EmbeddingModel;
import com.xjtutjc.service.ContextService;
import com.xjtutjc.service.FileIndexService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@Slf4j
public class SOPSubmitController {

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private FileIndexService fileIndexService;

    @PostMapping("/api/sop/submit")
    public ResponseEntity<?> submitSop(@RequestBody SopSubmitRequestDTO requestDTO){
        if(Objects.isNull(requestDTO) || StringUtils.isEmpty(requestDTO.getUserName()) || StringUtils.isEmpty(requestDTO.getContent())){
            return ResponseEntity.badRequest().body("提交sop请求失败，请求不能为空");
        }
        String content = requestDTO.getContent();
        float[] floatEmbedding = embeddingModel.getFloatEmbedding(content);
        Metadata metadata = new Metadata();
        metadata.put("userName",requestDTO.getUserName());
        metadata.put("timestamp",String.valueOf(System.currentTimeMillis()));
        TextSegment textSegment = new TextSegment(content,metadata);
        Embedding embedding = new Embedding(floatEmbedding);
        String addId = embeddingStore.add(embedding, textSegment);
        SopSubmitResponseDTO responseDTO = new SopSubmitResponseDTO();
        responseDTO.setSuccess(true);
        responseDTO.setAddId(addId);
        return ResponseEntity.ok(responseDTO);
    }
    @PostMapping("/api/sop/search")
    public ResponseEntity<?> searchSop(@RequestBody SopSearchRequestDTO searchRequestDTO){
        String searchContent = searchRequestDTO.getSearchContent();
        float[] floatEmbedding = embeddingModel.getFloatEmbedding(searchContent);
        Embedding embedding = new Embedding(floatEmbedding);
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(3)
                .minScore(0.75)
                .build();
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(searchRequest);
        SopSearchResponseDTO searchResponseDTO = new SopSearchResponseDTO();
        List<SearchResponse> list = new ArrayList<>();
        search.matches().stream().forEach(match -> {
            String text = match.embedded().text();
            Metadata metadata = match.embedded().metadata();
            String userName = metadata.getString("userName");
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setContent(text);
            searchResponse.setUserName(userName);
            list.add(searchResponse);
        });
        searchResponseDTO.setSearchResponseList(list);
        return ResponseEntity.ok(searchResponseDTO);
    }

    @PostMapping(value = "/api/sop/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file){
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ResponseEntity.badRequest().body("文件名不能为空");
        }
        try {
            String uploadPath = "./upload";
            Path uploadDir = Paths.get(uploadPath).normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // 使用原始文件名，而不是UUID，以便实现基于文件名的去重
            Path filePath = uploadDir.resolve(originalFilename).normalize();

            // 如果文件已存在，先删除旧文件（实现覆盖更新）
            if (Files.exists(filePath)) {
                log.info("文件已存在，将覆盖: {}", filePath);
                Files.delete(filePath);
            }

            Files.copy(file.getInputStream(), filePath);

            log.info("文件上传成功: {}", filePath);

            try {
                log.info("开始为上传文件创建向量索引: {}", filePath);
                fileIndexService.indexSingleFile(filePath.toString());
                log.info("向量索引创建成功: {}", filePath);
            } catch (Exception e) {
                log.error("向量索引创建失败: {}, 错误: {}", filePath, e.getMessage(), e);
                // 注意：即使索引失败，文件上传仍然成功，只是记录错误日志
                // 可以根据业务需求决定是否要删除文件或返回错误
            }
            FileUploadResponse fileUploadResponse = new FileUploadResponse();
            fileUploadResponse.setFileName(originalFilename);
            fileUploadResponse.setFilePath(filePath.toString());
            fileUploadResponse.setFileSize(file.getSize());

            // 使用统一的API响应格式
            ApiResponse<FileUploadResponse> apiResponse = new ApiResponse<>();
            apiResponse.setCode(200);
            apiResponse.setMessage("success");
            apiResponse.setData(fileUploadResponse);

            return ResponseEntity.ok(apiResponse);
        }catch (IOException e) {
            ApiResponse<String> errorResponse = new ApiResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage("文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
