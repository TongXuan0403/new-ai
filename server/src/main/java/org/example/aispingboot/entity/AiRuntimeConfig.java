package org.example.aispingboot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRuntimeConfig {
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String defaultModel;
}
