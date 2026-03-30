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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@Slf4j
public class ChatController {
    @Resource
    private ChatModel chatModel;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @RequestMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequsetDTO chatRequsetDTO){
        ApiResponse<ChatResponseDTO> apiResponse = new ApiResponse<>();
        String chat = chatModel.chat(chatRequsetDTO.getQuestion());
        ChatResponseDTO chatResponse = new ChatResponseDTO();
        chatResponse.setSuccess(true);
        chatResponse.setAnswer(chat);
        apiResponse.setData(chatResponse);
        apiResponse.setCode(200);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestBody ChatRequsetDTO chatRequsetDTO){
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        executor.execute(() -> {
            // 用于累积完整答案
            StringBuilder fullAnswerBuilder = new StringBuilder();
            Flowable<GenerationResult> generationResultFlowable = chatModel.streamChat(chatRequsetDTO.getQuestion());
            if(Objects.isNull(generationResultFlowable)){
                emitter.completeWithError(new RuntimeException("流式返回为空"));
            }
            generationResultFlowable.subscribe(message -> {
                String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                String finishReason = message.getOutput().getChoices().get(0).getFinishReason();
                // 实时发送到前端
                if (content != null && !content.isEmpty()){
                    fullAnswerBuilder.append(content);
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(SseMessage.content(content), MediaType.APPLICATION_JSON));
                }

                if (finishReason != null && !"null".equals(finishReason)) {
                    System.out.println("\n--- 请求用量 ---");
                    System.out.println("输入 Tokens：" + message.getUsage().getInputTokens());
                    System.out.println("输出 Tokens：" + message.getUsage().getOutputTokens());
                    System.out.println("总 Tokens：" + message.getUsage().getTotalTokens());
                }

            }, error -> {
                log.error("流式对话报错,error : {}", JSON.toJSONString(error));
                emitter.completeWithError(error);
            }, () -> {
                System.out.println("完整对话内容:"+fullAnswerBuilder.toString());
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(SseMessage.done(), MediaType.APPLICATION_JSON));
                emitter.complete();
            });
        });
        return emitter;
    }
}
