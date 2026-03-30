package com.xjtutjc.model;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.xjtutjc.config.ModelConfig;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class AliChatModel implements ChatModel{
    @Resource
    private ModelConfig modelConfig;
    private Message systemMsg = Message.builder().role(Role.SYSTEM.getValue())
            .content("你是一个架构师，当用户向你提问时，你需要根据架构师的思路去思考用户的问题并回答，要把回答中的引用文献标注出来可跳转的地址。").build();

    @Override
    public String chat(String msg) {
        Generation gen = new Generation();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(msg).build();
        GenerationParam generationParam = GenerationParam.builder()
                .apiKey(modelConfig.getAliqwenKey())
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg,userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        try {
            GenerationResult call = gen.call(generationParam);
            if (call.getStatusCode() != 200){
                log.error("模型调用失败, result = {}", JSON.toJSONString(call));
                return null;
            }
            GenerationOutput output = call.getOutput();
            return output.getChoices().get(0).getMessage().getContent();
        } catch (NoApiKeyException e) {
            log.error("apiKey调用失败,param : {},exception : {}",JSON.toJSONString(generationParam),JSON.toJSONString(e));
        } catch (InputRequiredException e) {
            log.error("输入报错,param : {},exception : {}",JSON.toJSONString(generationParam),JSON.toJSONString(e));
        }
        return null;
    }
    @Override
    public Flowable<GenerationResult> streamChat(String msg) {
        Generation gen = new Generation();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(msg).build();
        GenerationParam generationParam = GenerationParam.builder()
                .apiKey("sk-63ad8eb83cf746639f74f80fcdb984f6")
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg,userMsg))
                .incrementalOutput(true)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        try {
            Flowable<GenerationResult> result = gen.streamCall(generationParam);
            return result;
        } catch (NoApiKeyException e) {
            log.error("apiKey调用失败,param : {},exception : {}",JSON.toJSONString(generationParam),JSON.toJSONString(e));
        } catch (InputRequiredException e) {
            log.error("输入报错,param : {},exception : {}",JSON.toJSONString(generationParam),JSON.toJSONString(e));
        }
        return null;
    }
}
