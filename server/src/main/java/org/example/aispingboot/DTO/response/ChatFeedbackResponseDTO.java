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
public class ChatFeedbackResponseDTO {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Integer helpfulness;
    private String comment;
    private LocalDateTime createdAt;
}
