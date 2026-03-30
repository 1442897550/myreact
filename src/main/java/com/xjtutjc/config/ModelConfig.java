package com.xjtutjc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:model.properties")
public class ModelConfig {
    @Value("${model.config.aliqwenKey}")
    private String aliqwenKey;

    public String getAliqwenKey(){
        return aliqwenKey;
    }

}
