package com.xjtutjc.context;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ToolCallContext {
    private String name;
    private String args;
    private int index;
    private String id;
    private ToolCallFunction toolCallFunction;
}
