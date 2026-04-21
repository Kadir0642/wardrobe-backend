package com.vestify.backend.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                // 🚀 DÜZELTME: Docker içindeki servis adını kullanıyoruz!
                .baseUrl("http://python_ai_api:8000") 
                .build();
    }
}