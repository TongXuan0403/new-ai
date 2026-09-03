package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 情绪日记分页返回
 */
@Data
@Builder
public class EmotionDiaryPageVO {
    private List<EmotionDiaryVO> records;
    private Long total;
}
