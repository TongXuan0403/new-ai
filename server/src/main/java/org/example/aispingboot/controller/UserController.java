package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.UserLoginCommandDTO;
import org.example.aispingboot.DTO.command.UserRegisterCommandDTO;
import org.example.aispingboot.DTO.response.UserLoginResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.TokenBlacklistService;
import org.example.aispingboot.service.UserService;
import org.example.aispingboot.util.JwtTokenUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;

    public UserController(UserService userService, TokenBlacklistService tokenBlacklistService) {
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO command) {
        return Result.ok(userService.login(command), "登录成功");
    }

    @PostMapping("/add")
    public Result<UserLoginResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO command) {
        return Result.ok(userService.register(command), "注册成功");
    }

    @PostMapping("/logout")
    public Result<Boolean> logout() {
        tokenBlacklistService.blacklist(JwtTokenUtil.getCurrentToken());
        return Result.ok(true, "退出成功");
    }
}
