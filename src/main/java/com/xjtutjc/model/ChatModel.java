package com.xjtutjc.model;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.xjtutjc.context.ChatContext;
import io.reactivex.Flowable;

import java.util.List;

public interface ChatModel {

    public Flowable<GenerationResult> streamChat(ChatContext context);
}
