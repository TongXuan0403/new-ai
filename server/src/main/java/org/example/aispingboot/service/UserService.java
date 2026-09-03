package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.command.UserLoginCommandDTO;
import org.example.aispingboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispingboot.DTO.response.UserLoginResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.UserMapper;
import org.example.aispingboot.util.JwtTokenUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public UserLoginResponseDTO register(UserRegisterCommandDTO command) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, command.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_SAME);
        }
        User user = User.builder()
                .username(command.getUsername())
                .password(passwordEncoder.encode(command.getPassword()))
                .nickname(StringUtils.hasText(command.getNickname()) ? command.getNickname() : command.getUsername())
                .gender(0)
                .userType(1)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userMapper.insert(user);
        return buildLoginResponse(user);
    }

    public UserLoginResponseDTO login(UserLoginCommandDTO command) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, command.getUsername()));
        if (user == null || !passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "用户名或密码错误");
        }
        if (user.isDisabled()) {
            throw new BusinessException(ResultCode.TOKEN_ACCESS_FORBIDDEN);
        }
        return buildLoginResponse(user);
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public User getEntityById(Long id) {
        return userMapper.selectById(id);
    }

    private UserLoginResponseDTO buildLoginResponse(User user) {
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType(), user.getStatus());
        return UserLoginResponseDTO.builder()
                .token(token)
                .roleType(String.valueOf(user.getUserType()))
                .userInfo(UserLoginResponseDTO.UserDetailResponseDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .gender(user.getGender())
                        .userType(user.getUserType())
                        .status(user.getStatus())
                        .displayName(user.getDisplayName())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }
}
