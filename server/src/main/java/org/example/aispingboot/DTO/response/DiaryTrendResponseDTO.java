package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 日记趋势：只描述记录变化，不推断疾病。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTrendResponseDTO {
    private Integer days;
    private Long recordCount;
    private List<Point> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private LocalDate date;
        private Integer score;
        private String emotionStatus;
    }
}
