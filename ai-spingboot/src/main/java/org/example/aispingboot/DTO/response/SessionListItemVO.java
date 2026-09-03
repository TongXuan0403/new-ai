package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项
 */
@Data
@Builder
public class SessionListItemVO {
    private Long id;
    private String sessionTitle;
    private String username;
    private String userNickname;
    private LocalDateTime startedAt;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer messageCount;
    private Long durationMinutes;
}
