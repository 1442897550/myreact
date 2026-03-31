package com.xjtutjc.contoller;

import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.fastjson.JSON;
import com.xjtutjc.context.ChatContext;
import com.xjtutjc.dto.request.ChatRequsetDTO;
import com.xjtutjc.dto.response.SseMessage;
import com.xjtutjc.model.ChatModel;
import com.xjtutjc.service.ChatService;
import com.xjtutjc.service.ToolCallService;
import com.xjtutjc.tools.WeatherTool;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Slf4j
public class ChatController {
    
    @Resource
    private ChatModel chatModel;

    @Resource
    private ChatService chatService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 访问聊天页面
     */
    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("timestamp", System.currentTimeMillis());
        return "chat";
    }

    @PostMapping(value = "/api/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(@RequestBody ChatRequsetDTO chatRequsetDTO){
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(300000L); // 5 分钟超时

        // 添加超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，sessionId: {}", chatRequsetDTO.getId());
            try {
                emitter.send(SseMessage.error("请求超时，请重试"));
            } catch (Exception e) {
                log.error("发送超时消息失败", e);
            }
            emitter.complete();
        });

        // 添加完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成，sessionId: {}", chatRequsetDTO.getId());
        });

        // 添加错误回调
        emitter.onError(throwable -> {
            log.error("SSE 连接错误，sessionId: {}, error: {}", chatRequsetDTO.getId(), throwable.getMessage());
        });

        executor.execute(() -> {
            // 用于累积完整答案
            StringBuilder fullAnswerBuilder = new StringBuilder();
            Message userMessage = Message.builder().content(chatRequsetDTO.getQuestion()).role(Role.USER.getValue()).build();
            ChatContext chatContext = new ChatContext();
            chatContext.getMessages().add(userMessage);
            Message systemMessage = Message.builder().role(Role.SYSTEM.getValue())
                    .content("你是一个架构师，当用户向你提问时，你需要根据架构师的思路去思考用户的问题并回答，要把回答中的引用文献标注出来可跳转的地址。").build();
            chatContext.getMessages().add(systemMessage);
            Flowable<GenerationResult> generationResultFlowable = chatModel.streamChat(chatContext);
            if(Objects.isNull(generationResultFlowable)){
                try {
                    emitter.send(SseMessage.error("流式返回为空"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                return;
            }
            //进行工具调用
            Flowable<GenerationResult> afterToolResult = chatService.recurToolCallStream(generationResultFlowable, chatContext);

            afterToolResult.subscribe(message -> {
                GenerationOutput.Choice choice = message.getOutput().getChoices().get(0);
                Message realMessage = choice.getMessage();
                String content = realMessage.getContent();
                String finishReason = choice.getFinishReason();
                if (content != null && !content.isEmpty()){
                    fullAnswerBuilder.append(content);
                    try {
                        emitter.send(SseMessage.content(content));
                    } catch (Exception e) {
                        log.error("发送消息失败", e);
                        emitter.completeWithError(e);
                    }
                }

                if (finishReason != null && !"null".equals(finishReason)) {
                    System.out.println("\n--- 请求用量 ---");
                    System.out.println("输入 Tokens：" + message.getUsage().getInputTokens());
                    System.out.println("输出 Tokens：" + message.getUsage().getOutputTokens());
                    System.out.println("总 Tokens：" + message.getUsage().getTotalTokens());
                }

            }, error -> {
                log.error("流式对话报错，error : {}", JSON.toJSONString(error));
                try {
                    emitter.send(SseMessage.error(error.getMessage()));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }, () -> {
                System.out.println("完整对话内容：" + fullAnswerBuilder.toString());
                try {
                    emitter.send(SseMessage.done());
                    emitter.complete();
                } catch (Exception e) {
                    log.error("发送完成消息失败", e);
                }
            });
        });
        return emitter;
    }
}
