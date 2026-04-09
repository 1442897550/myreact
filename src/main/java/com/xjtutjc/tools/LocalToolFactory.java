package com.xjtutjc.tools;

import com.alibaba.dashscope.tools.ToolBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class LocalToolFactory {
    private static Map<String, LocalTool> toolMap = new HashMap<>();
    private static Map<String, ToolCallback> toolCallbackMap = new HashMap<>();
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    @Resource
    private ApplicationContext applicationContext;
    @PostConstruct
    private void buildMap(){
        Map<String, LocalTool> beansOfType = applicationContext.getBeansOfType(LocalTool.class);
        for (String s : beansOfType.keySet()) {
            toolMap.put(beansOfType.get(s).getToolName(),beansOfType.get(s));
        }
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        for (ToolCallback toolCallback : toolCallbacks) {
            toolCallbackMap.put(toolCallback.getToolDefinition().name(),toolCallback);
        }
    }
    public LocalTool getTool(String toolName){
        LocalTool localTool = toolMap.get(toolName);
        if(Objects.isNull(localTool)){
            log.error("根据toolName获取对应tool实例为空,toolName = {}",toolName);
            throw new RuntimeException("未定义的toolname"+toolName);
        }
        return localTool;
    }

    public ToolCallback getToolCallBack(String toolName){
        ToolCallback toolCallback = toolCallbackMap.get(toolName);
        return toolCallback;
    }

    public List<ToolBase> getToolBaseList(){
        return toolMap.values().stream().map(tool -> (ToolBase)tool.getToolFunction()).toList();
    }
}
