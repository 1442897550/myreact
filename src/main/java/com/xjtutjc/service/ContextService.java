package com.xjtutjc.service;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.fastjson.JSON;
import com.xjtutjc.context.ChatContext;
import com.xjtutjc.dto.response.SearchResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 上下文service，构建上下文，包含系统上下文，用户缓存上下文，rag上下文
 */
@Service
public class ContextService {
    @Resource
    private QueryService queryService;
    public ChatContext buildContext(String userPrompt,List<Map<String, String>> history){
        ChatContext chatContext = new ChatContext();

        // 添加系统prompt
        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append("上下文理解与对话\n");
        systemPromptBuilder.append("核心能力：上下文理解与对话\n");
        systemPromptBuilder.append("互动指南:在回复前，请确保你：1. 完全理解用户的需求和问题，如果有不清楚的地方，要向用户确认。 2. 考虑最合适的解决方案方法。在提供帮助时：1. 语言清晰简洁。2. 适当的时候提供实际例子。3. 可以查询参考文档。4. 适用时建议改进或下一步操作。如果请求超出了你的能力范围：1. 清晰地说明你的局限性，如果可能的话，建议其他方法。如果问题是复合或复杂的，你需要一步步思考，避免直接给出质量不高的回答。\n");
        systemPromptBuilder.append("输出要求：1. 易读，结构良好，必要时换行。2. 输出不能包含markdown的语法，输出需要纯文本。\n");
        //第一次对话前检索
        List<SearchResponse> list = queryService.queryInternalDocs(userPrompt);
        systemPromptBuilder.append("参考文档：");
        systemPromptBuilder.append(JSON.toJSONString(list));
        // 添加历史消息
        if (!history.isEmpty()) {
            systemPromptBuilder.append("--- 对话历史 ---\n");
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    systemPromptBuilder.append("用户: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    systemPromptBuilder.append("助手: ").append(content).append("\n");
                }
            }
            systemPromptBuilder.append("--- 对话历史结束 ---\n\n");
        }

        systemPromptBuilder.append("请基于以上对话历史，回答用户的新问题。");
        Message systemMessage = Message.builder().role(Role.SYSTEM.getValue())
                .content(systemPromptBuilder.toString()).build();
        chatContext.getMessages().add(systemMessage);
        // 添加userPrompt
        Message userMessage = Message.builder().content(userPrompt).role(Role.USER.getValue()).build();
        chatContext.getMessages().add(userMessage);
        return chatContext;
    }
}
