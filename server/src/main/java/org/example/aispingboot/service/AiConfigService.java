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

    private final ConfigVersionService configVersionService;

    public AiConfigService(ConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    public AiRuntimeConfig resolveRuntimeConfig() {
        return AiRuntimeConfig.builder()
                .provider(provider)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .defaultModel(resolveActiveModel())
                .build();
    }

    /**
     * 生效模型名：优先使用后台生效的 MODEL 版本内容，否则回退环境变量默认值。
     */
    public String resolveActiveModel() {
        String active = configVersionService.getActiveContent(ConfigVersionService.TYPE_MODEL, "");
        if (StringUtils.hasText(active)) {
            return active.trim();
        }
        return defaultModel;
    }

    /**
     * 当前生效模型版本号（用于消息/风险事件可追溯）。
     */
    public String modelVersionLabel() {
        return configVersionService.getActiveVersionLabel(ConfigVersionService.TYPE_MODEL, "model-v1.0");
    }

    /**
     * 归一化请求的模型名：为空或用默认模型时返回生效模型。
     */
    public String normalizeRequestedModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel;
        }
        return resolveActiveModel();
    }
}
