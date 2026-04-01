package com.xjtutjc.model;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.xjtutjc.config.ModelConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AliQwenEmbeddingModel implements EmbeddingModel{
    private TextEmbedding textEmbedding;
    @Resource
    private ModelConfig modelConfig;
    @PostConstruct
    public void init(){
        Constants.apiKey = modelConfig.getAliqwenKey();
        this.textEmbedding = new TextEmbedding();
    }
    @Override
    public List<Double> generateEmbedding(String text) {
        try {
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .model("text-embedding-v4")
                    // 输入文本
                    .texts(Collections.singleton(text))
                    .build();
            TextEmbeddingResult result = textEmbedding.call(param);
            if (result == null){
                return Collections.EMPTY_LIST;
            }
            return result.getOutput().getEmbeddings().get(0).getEmbedding();
        }catch (NoApiKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float[] getFloatEmbedding(String text){
        List<Double> doubleVectorList = generateEmbedding(text);
        if(CollectionUtils.isEmpty(doubleVectorList)){
            throw new RuntimeException("向量返回为空");
        }
        float[] vectors = new float[doubleVectorList.size()];
        for(int i = 0; i < doubleVectorList.size(); i++){
            vectors[i] = doubleVectorList.get(i).floatValue();
        }
        return vectors;
    }
}
