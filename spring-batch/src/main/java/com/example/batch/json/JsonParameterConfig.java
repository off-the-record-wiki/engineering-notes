package com.example.batch.json;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonParameterConfig {

    @Bean
    public JsonJobParametersReader jsonJobParametersReader() {
        return new JsonJobParametersReader();
    }
}