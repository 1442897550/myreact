package com.xjtutjc.service;

import com.alibaba.fastjson.JSON;
import com.xjtutjc.context.ToolCallContext;
import com.xjtutjc.tools.LocalTool;
import com.xjtutjc.tools.LocalToolFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ToolCallService {
    @Resource
    private LocalToolFactory localToolFactory;
    public String getResult(ToolCallContext context){
        try {
            ToolCallback toolCallBack = localToolFactory.getToolCallBack(context.getName());
            return toolCallBack.call(context.getArgs());

        }catch (Exception e){
            log.error("调用tool失败,e = {}", JSON.toJSON(e));
            return null;
        }
    }
}
