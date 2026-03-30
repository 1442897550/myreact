package com.xjtutjc.contoller;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.fastjson.JSON;
import com.xjtutjc.dto.request.ChatRequsetDTO;
import com.xjtutjc.dto.response.ApiResponse;
import com.xjtutjc.dto.response.ChatResponseDTO;
import com.xjtutjc.dto.response.SseMessage;
import com.xjtutjc.model.ChatModel;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@Slf4j
public class ChatController {
    
    @Resource
    private ChatModel chatModel;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 访问聊天页面
     */
    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }

    @RestController
    @RequestMapping("/api")
    static class ApiController {
        
        @Resource
        private ChatModel chatModel;

        private final ExecutorService executor = Executors.newCachedThreadPool();

        @RequestMapping("/chat")
        public ApiResponse<ChatResponseDTO> chat(@RequestBody ChatRequsetDTO chatRequsetDTO){
            ApiResponse<ChatResponseDTO> apiResponse = new ApiResponse<>();
            String chat = chatModel.chat(chatRequsetDTO.getQuestion());
            ChatResponseDTO chatResponse = new ChatResponseDTO();
            chatResponse.setSuccess(true);
            chatResponse.setAnswer(chat);
            apiResponse.setData(chatResponse);
            apiResponse.setCode(200);
            return apiResponse;
        }

        @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
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
                Flowable<GenerationResult> generationResultFlowable = chatModel.streamChat(chatRequsetDTO.getQuestion());
                if(Objects.isNull(generationResultFlowable)){
                    try {
                        emitter.send(SseMessage.error("流式返回为空"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                    return;
                }
                generationResultFlowable.subscribe(message -> {
                    String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                    String finishReason = message.getOutput().getChoices().get(0).getFinishReason();
                    // 实时发送到前端
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
}
