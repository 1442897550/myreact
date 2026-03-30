package com.xjtutjc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "model.config")
public class ModelConfig {
    private String aliqwenKey;

    public String getAliqwenKey(){
        return aliqwenKey;
    }
}
