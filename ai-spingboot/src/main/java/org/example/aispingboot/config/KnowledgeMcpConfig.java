package org.example.aispingboot.config;

import org.example.aispingboot.service.KnowledgeBaseService;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeMcpConfig {

    @Bean
    public ToolCallbackProvider knowledgeToolCallbackProvider(KnowledgeBaseService knowledgeBaseService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeBaseService)
                .build();
    }
}
