package org.example.aispingboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * 生成 token
     */
    public String generateToken(Long userId, String username, Integer userType, Integer status) {
        Date now = new Date();
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("userType", userType)
                .withClaim("status", status)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expiration))
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 生成刷新 token
     */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + refreshExpiration))
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 验证 token，返回解析结果；无效时抛出业务异常
     */
    public DecodedJWT verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            return verifier.verify(token);
        } catch (TokenExpiredException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (JWTDecodeException | IllegalArgumentException e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    /**
     * 从当前请求头解析 token（去掉 Bearer 前缀）
     */
    public static String getCurrentToken() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader;
    }
}
