package com.xjtutjc.model;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.xjtutjc.context.ChatContext;
import io.reactivex.Flowable;

public interface ChatModel {

    public Flowable<GenerationResult> streamChat(ChatContext context);
}
