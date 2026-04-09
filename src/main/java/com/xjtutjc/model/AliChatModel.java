package com.xjtutjc.model;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSON;
import com.xjtutjc.config.ModelConfig;
import com.xjtutjc.context.ChatContext;
import com.xjtutjc.tools.CurrentTimeTool;
import com.xjtutjc.tools.LocalToolFactory;
import com.xjtutjc.tools.McpToolService;
import com.xjtutjc.tools.WeatherTool;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class AliChatModel implements ChatModel{
    @Resource
    private ModelConfig modelConfig;
    @Resource
    private LocalToolFactory localToolFactory;
    @Resource
    private McpToolService mcpToolService;
    @Autowired
    private ToolCallbackProvider tools;
    @Override
    public Flowable<GenerationResult> streamChat(ChatContext context) {
        Generation gen = new Generation();

        GenerationParam generationParam = GenerationParam.builder()
                .apiKey(modelConfig.getAliqwenKey())
                .model("qwen-plus")
                .messages(context.getMessages())
                .incrementalOutput(true)
                .tools(mcpToolService.getToolBaseList())
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
