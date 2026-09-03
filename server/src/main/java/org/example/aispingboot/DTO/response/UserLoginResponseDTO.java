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
public class UserLoginResponseDTO {
    private String token;
    private String roleType;
    private UserDetailResponseDTO userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetailResponseDTO {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private Integer gender;
        private Integer userType;
        private Integer status;
        private String displayName;
        private LocalDateTime createdAt;
    }
}
