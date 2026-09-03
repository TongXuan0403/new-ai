package org.example.aispingboot.service;

import org.example.aispingboot.entity.AiRuntimeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiConfigService {

    @Value("${ai.provider:siliconflow}")
    private String provider;

    @Value("${ai.base-url:}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.default-model:deepseek-ai/DeepSeek-V3}")
    private String defaultModel;

    public AiRuntimeConfig resolveRuntimeConfig() {
        return AiRuntimeConfig.builder()
                .provider(provider)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultModel(defaultModel)
                .build();
    }

    /**
     * 归一化请求的模型名：为空或用默认模型时返回默认模型。
     */
    public String normalizeRequestedModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel;
        }
        return defaultModel;
    }
}
