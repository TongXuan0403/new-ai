package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseCompletionResponseDTO {
    private Long id;
    private Long exerciseId;
    private String exerciseTitle;
    private String exerciseSummary;
    private Integer minutes;
    private String tags;
    private String moodAfter;
    private LocalDateTime completedAt;
}
