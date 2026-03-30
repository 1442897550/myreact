package com.xjtutjc.config;

import org.springframework.stereotype.Component;

@Component
public class PromptConfig {
    private String systemPrompt;
    public String getSystemPrompt(){
        return this.systemPrompt;
    }
}
