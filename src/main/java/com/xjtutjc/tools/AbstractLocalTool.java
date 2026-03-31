package com.xjtutjc.tools;

import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import jakarta.annotation.PostConstruct;

public abstract class AbstractLocalTool implements LocalTool{

    private ToolFunction toolFunction;

    @PostConstruct
    public void buildFunction(){
        this.toolFunction = ToolFunction.builder().function(FunctionDefinition.builder()
                .name(getToolName()) // 工具的唯一标识名，必须与本地实现对应。
                .description(getToolDescription()) // 清晰的描述能帮助模型更好地决定何时使用该工具。
                .parameters(JsonUtils.parseString(getToolSchema()).getAsJsonObject())
                .build()).build();
    }

    public ToolFunction getToolFunction(){
        return this.toolFunction;
    }
}
