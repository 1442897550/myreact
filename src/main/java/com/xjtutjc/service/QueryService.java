package com.xjtutjc.service;

import com.alibaba.fastjson.JSONObject;
import com.xjtutjc.dto.response.SearchResponse;
import com.xjtutjc.model.EmbeddingModel;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService {
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;
    @Resource
    private EmbeddingModel embeddingModel;

    public List<SearchResponse> queryInternalDocs(String content) {
        float[] floatEmbedding = embeddingModel.getFloatEmbedding(content);
        Embedding embedding = new Embedding(floatEmbedding);
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(3)
                .minScore(0.75)
                .build();
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(searchRequest);
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
        return list;
    }
}
