package com.xjtutjc.tools;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Service
public class CurrentTimeTool extends AbstractLocalTool{
    @Override
    public String getToolResult(String args) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(dateTimeFormatter);
    }

    @Override
    public String getToolName() {
        return "date_time";
    }

    @Override
    public String getToolSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
    }

    @Override
    public String getToolDescription() {
        return "当你想查询当前时间时非常有用。";
    }
}
