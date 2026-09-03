package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.aispingboot.enumClass.UserStatus;
import org.example.aispingboot.enumClass.UserType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    private String password;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String bio;

    @TableField("user_type")
    private Integer userType;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public boolean isUser() {
        return UserType.USER.getCode().equals(this.userType);
    }

    public boolean isActive() {
        return UserStatus.NORMAL.getCode().equals(this.status);
    }

    public boolean isDisabled() {
        return UserStatus.DISABLED.getCode().equals(this.status);
    }

    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }
}
