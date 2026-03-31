package com.xjtutjc.tools;

import com.alibaba.dashscope.tools.ToolFunction;

public interface LocalTool {
    public ToolFunction getToolFunction();
    public String getToolResult(String args);
    public String getToolName();
    public String getToolSchema();
    public String getToolDescription();
}
