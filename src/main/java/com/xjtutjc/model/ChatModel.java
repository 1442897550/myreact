package com.xjtutjc.model;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import io.reactivex.Flowable;

public interface ChatModel {
    public String chat(String msg);

    public Flowable<GenerationResult> streamChat(String msg);
}
