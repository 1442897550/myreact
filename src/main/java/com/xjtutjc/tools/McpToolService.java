package com.xjtutjc.tools;

import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class McpToolService {
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    public List<ToolBase> getToolBaseList(){
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        List<ToolBase> res = new ArrayList<>();
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            ToolFunction build = ToolFunction.builder().function(FunctionDefinition.builder()
                    .name(toolDefinition.name()) // 工具的唯一标识名，必须与本地实现对应。
                    .description(toolDefinition.description()) // 清晰的描述能帮助模型更好地决定何时使用该工具。
                    .parameters(JsonUtils.parseString(toolDefinition.inputSchema()).getAsJsonObject())
                    .build()).build();
            res.add(build);
        }
        return res;

    }
}
