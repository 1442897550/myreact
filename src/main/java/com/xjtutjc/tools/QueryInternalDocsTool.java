package com.xjtutjc.tools;

import com.alibaba.fastjson.JSONObject;
import com.xjtutjc.dto.response.SearchResponse;
import com.xjtutjc.model.EmbeddingModel;
import com.xjtutjc.service.QueryService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class QueryInternalDocsTool extends AbstractLocalTool{
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private EmbeddingModel embeddingModel;
    @Resource
    private QueryService queryService;
    @Override
    public String getToolResult(String args) {
        JSONObject arguments = JSONObject.parseObject(args);
        String query = arguments.getString("query");
        List<SearchResponse> list = queryService.queryInternalDocs(query);
        return CollectionUtils.isEmpty(list) ? "" : JSONObject.toJSONString(list);
    }

    @Override
    public String getToolName() {
        return "query_internal_docs";
    }

    @Override
    public String getToolSchema() {
        return "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"需要查询的问题\"}},\"required\":[\"query\"]}";
    }

    @Override
    public String getToolDescription() {
        return "当你想查询参考文档文档时非常有用。";
    }
}
