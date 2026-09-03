package org.example.aispingboot.util;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.aispingboot.DTO.response.UserLoginResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.config.SecurityConfig;
import org.example.aispingboot.enumClass.UserStatus;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.UserService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthticationFilter extends OncePerRequestFilter {
    @Resource
    private UserService userService;
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // 检查是否为公开路径
        return SecurityConfig.isPublicPATH(requestUri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            // 1. 提取 JWT token
            String token = JwtTokenUtil.extractTokenFromRequest(request);
            if (!StringUtils.hasText(token)) {
                clearSecurityContext();
                ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
                return;
            }

            // 2. 验证token并获取用户信息
            JwtTokenUtil.TokenVerificationResult validationResult;
            try {
                validationResult = JwtTokenUtil.validateToken(token);
            } catch (JWTVerificationException e) {
                clearSecurityContext();
                ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
                return;
            }

            if (validationResult == null || !validationResult.isValid()) {
                clearSecurityContext();
                ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
                return;
            }

            UserLoginResponseDTO.UserDetailResponseDTO user;
            try {
                user = userService.getUserById(validationResult.getUserId());
            } catch (BusinessException e) {
                clearSecurityContext();
                ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
                return;
            }

            if (user != null && UserStatus.NORMAL.getCode().equals(user.getStatus())) {
                // 4. 创建Spring Security认证对象
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + validationResult.getRoleType())
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        validationResult.getUsername(),
                        null,
                        authorities
                );

                // 设置认证信息到Spring Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 将token存储到请求属性中
                request.setAttribute("jwtToken", token);
                chain.doFilter(request, response);
                return;
            }

            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
        } catch (Exception e) {
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.SYSTEM_ERROR);
        }
    }

    // 清理Spring Security上下文
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
