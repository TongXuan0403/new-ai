package org.example.aispingboot.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.aispingboot.config.SecurityConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器：从请求头解析 token，校验后写入 SecurityContext。
 */
public class JwtAuthticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // 公开路径直接放行
        if (SecurityConfig.isPublicPATH(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = JwtTokenUtil.getCurrentToken();
        if (StringUtils.hasText(token)) {
            try {
                DecodedJWT jwt = jwtTokenUtil.verifyToken(token);
                Long userId = jwt.getClaim("userId").asLong();
                Integer userType = jwt.getClaim("userType").asInt();
                Integer status = jwt.getClaim("status").asInt();
                if (userId != null && status != null && status == 1) {
                    String role = (userType != null && userType == 2) ? "ROLE_ADMIN" : "ROLE_USER";
                    List<SimpleGrantedAuthority> authorities =
                            Collections.singletonList(new SimpleGrantedAuthority(role));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                // token 无效时不设置认证，由后续鉴权拒绝
            }
        }
        filterChain.doFilter(request, response);
    }
}
