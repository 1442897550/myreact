package com.xjtutjc.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class LocalToolFactory {
    private static Map<String, LocalTool> toolMap = new HashMap<>();
    @Resource
    private ApplicationContext applicationContext;
    @PostConstruct
    private void buildMap(){
        Map<String, LocalTool> beansOfType = applicationContext.getBeansOfType(LocalTool.class);
        for (String s : beansOfType.keySet()) {
            toolMap.put(beansOfType.get(s).getToolName(),beansOfType.get(s));
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
}
