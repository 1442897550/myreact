package com.xjtutjc.model;

import java.util.List;

public interface EmbeddingModel {
    public List<Double> generateEmbedding(String text);
    public float[] getFloatEmbedding(String text);
}
