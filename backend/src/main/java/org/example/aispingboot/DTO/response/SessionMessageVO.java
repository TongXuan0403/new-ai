package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息
 */
@Data
@Builder
public class SessionMessageVO {
    private Long id;
    private Integer senderType;
    private String content;
    private LocalDateTime createdAt;
}
