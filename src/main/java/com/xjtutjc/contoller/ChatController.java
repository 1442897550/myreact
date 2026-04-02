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
import com.xjtutjc.service.ContextService;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Controller
@Slf4j
public class ChatController {
    
    @Resource
    private ChatModel chatModel;

    @Resource
    private ChatService chatService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Resource
    private ContextService contextService;

    // 存储会话信息
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // 最大历史消息窗口大小（成对计算：用户消息+AI回复=1对）
    private static final int MAX_WINDOW_SIZE = 6;

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

            // 获取或创建会话
            SessionInfo session = getOrCreateSession(chatRequsetDTO.getId());
            // 获取历史消息
            List<Map<String, String>> history = session.getHistory();
            log.info("ReactAgent 会话历史消息对数: {}", history.size() / 2);


            // 用于累积完整答案
            StringBuilder fullAnswerBuilder = new StringBuilder();
            ChatContext chatContext = contextService.buildContext(chatRequsetDTO.getQuestion(),history);
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
                chatService.recurToolCallStream(generationResultFlowable, chatContext);
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
                // 更新会话历史
                session.addMessage(chatRequsetDTO.getQuestion(), fullAnswerBuilder.toString());
                log.info("已更新会话历史 - SessionId: {}, 当前消息对数: {}",
                        chatRequsetDTO.getId(), session.getMessagePairCount());
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

    /**
     * 会话信息
     * 管理单个会话的历史消息，支持自动清理和线程安全
     */
    private static class SessionInfo {
        private final String sessionId;
        // 存储历史消息对：[{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
        private final List<Map<String, String>> messageHistory;
        private final long createTime;
        private final ReentrantLock lock;

        public SessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.messageHistory = new ArrayList<>();
            this.createTime = System.currentTimeMillis();
            this.lock = new ReentrantLock();
        }

        /**
         * 添加一对消息（用户问题 + AI回复）
         * 自动管理历史消息窗口大小
         */
        public void addMessage(String userQuestion, String aiAnswer) {
            lock.lock();
            try {
                // 添加用户消息
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", userQuestion);
                messageHistory.add(userMsg);

                // 添加AI回复
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", aiAnswer);
                messageHistory.add(assistantMsg);

                // 自动清理：保持最多 MAX_WINDOW_SIZE 对消息
                // 每对消息包含2条记录（user + assistant）
                int maxMessages = MAX_WINDOW_SIZE * 2;
                while (messageHistory.size() > maxMessages) {
                    // 成对删除最旧的消息（删除前2条）
                    messageHistory.remove(0); // 删除最旧的用户消息
                    if (!messageHistory.isEmpty()) {
                        messageHistory.remove(0); // 删除对应的AI回复
                    }
                }

                log.debug("会话 {} 更新历史消息，当前消息对数: {}",
                        sessionId, messageHistory.size() / 2);

            } finally {
                lock.unlock();
            }
        }

        /**
         * 获取历史消息（线程安全）
         * 返回副本以避免并发修改
         */
        public List<Map<String, String>> getHistory() {
            lock.lock();
            try {
                return new ArrayList<>(messageHistory);
            } finally {
                lock.unlock();
            }
        }

        /**
         * 清空历史消息
         */
        public void clearHistory() {
            lock.lock();
            try {
                messageHistory.clear();
                log.info("会话 {} 历史消息已清空", sessionId);
            } finally {
                lock.unlock();
            }
        }

        /**
         * 获取当前消息对数
         */
        public int getMessagePairCount() {
            lock.lock();
            try {
                return messageHistory.size() / 2;
            } finally {
                lock.unlock();
            }
        }
    }

    private SessionInfo getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        return sessions.computeIfAbsent(sessionId, SessionInfo::new);
    }
}
