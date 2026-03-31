package com.xjtutjc.service;

import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.xjtutjc.context.ChatContext;
import com.xjtutjc.context.ToolCallContext;
import com.xjtutjc.model.ChatModel;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {
    @Resource
    private ChatModel chatModel;
    @Resource
    private ToolCallService toolCallService;

    public Flowable<GenerationResult> recurToolCallStream(Flowable<GenerationResult> flowable,ChatContext chatContext){
        //递归调用当前函数，判断是否要执行工具
        //拿到所有结果，合并后进行工具调用，将工具调用结果放入上下文，重新请求模型，再进行递归
        return flowable.toList().flatMapPublisher(results -> {
            Map<Integer, ToolCallContext> toolCallContextMap = new HashMap<>();
            for (GenerationResult result : results) {
                GenerationOutput output = result.getOutput();
                GenerationOutput.Choice choice = output.getChoices().get(0);
                Message message = choice.getMessage();
                if(CollectionUtils.isEmpty(message.getToolCalls())){
                    return flowable;
                }
                ToolCallBase toolCall = message.getToolCalls().get(0);
                ToolCallFunction functionCall = (ToolCallFunction) toolCall;
                ToolCallContext context = toolCallContextMap.getOrDefault(functionCall.getIndex(), new ToolCallContext());
                if (!StringUtils.isEmpty(functionCall.getFunction().getName())) {
                    context.setName(functionCall.getFunction().getName());
                }
                if (!StringUtils.isEmpty(functionCall.getId())) {
                    context.setId(functionCall.getId());
                }
                if(StringUtils.isEmpty(context.getArgs())){
                    context.setArgs(functionCall.getFunction().getArguments());
                }else{
                    context.setArgs(context.getArgs() + functionCall.getFunction().getArguments());
                }
                context.getToolCallBaseList().add(functionCall);
                context.setIndex(functionCall.getIndex());
                toolCallContextMap.put(functionCall.getIndex(),context);
                System.out.println("工具添加" + functionCall.getId() + "工具参数" + functionCall.getFunction().getArguments() + "工具目标调用" + functionCall.getFunction().getName());
            }
            for (Integer index : toolCallContextMap.keySet()) {
                ToolCallContext context = toolCallContextMap.get(index);
                String result = toolCallService.getResult(toolCallContextMap.get(index));
                Message assistMessage = Message.builder()
                        .toolCalls(context.getToolCallBaseList())
                        .role(Role.ASSISTANT.getValue())
                        .toolCallId(context.getId())
                        .build();
                chatContext.getMessages().add(assistMessage);
                Message toolMessage = Message
                        .builder()
                        .toolCalls(context.getToolCallBaseList())
                        .toolCallId(context.getId())
                        .role(Role.TOOL.getValue())
                        .content(result)
                        .build();
                chatContext.getMessages().add(toolMessage);
            }
            Flowable<GenerationResult> generationResultFlowable = chatModel.streamChat(chatContext);
            return recurToolCallStream(generationResultFlowable,chatContext);
        });
    }
}
