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
import com.xjtutjc.context.ChatContext;
import com.xjtutjc.tool.WeatherTool;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AliChatModel implements ChatModel{
    @Resource
    private ModelConfig modelConfig;
    @Resource
    private WeatherTool weatherTool;
    private Message systemMsg = Message.builder().role(Role.SYSTEM.getValue())
            .content("你是一个架构师，当用户向你提问时，你需要根据架构师的思路去思考用户的问题并回答，要把回答中的引用文献标注出来可跳转的地址。").build();
    @Override
    public Flowable<GenerationResult> streamChat(ChatContext context) {
        Generation gen = new Generation();
        GenerationParam generationParam = GenerationParam.builder()
                .apiKey(modelConfig.getAliqwenKey())
                .model("qwen-plus")
                .messages(context.getMessages())
                .incrementalOutput(true)
                .tools(Arrays.asList(weatherTool.getToolFunction()))
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
