package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 会话情绪分析结果
 */
@Data
@Builder
public class SessionEmotionVO {
    private String primaryEmotion;
    private Integer emotionScore;
    private Boolean isNegative;
    private Integer riskLevel;
    private String suggestion;
    private List<String> improvementSuggestions;
    private String riskDescription;
}
