package org.example.aispingboot.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户上下文工具
 */
public class UserContext {

    private UserContext() {
    }

    public static DecodedJWT currentJwt() {
        String token = JwtTokenUtil.getCurrentToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return JwtTokenUtil.verifyToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static Long getCurrentUserId() {
        DecodedJWT jwt = currentJwt();
        return jwt != null ? jwt.getClaim("userId").asLong() : null;
    }

    public static String getCurrentUsername() {
        DecodedJWT jwt = currentJwt();
        return jwt != null ? jwt.getClaim("username").asString() : null;
    }

    public static boolean isAdmin() {
        DecodedJWT jwt = currentJwt();
        return jwt != null && Integer.valueOf(2).equals(jwt.getClaim("roleType").asInt());
    }
}
