package org.example.aispingboot.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单。MVP 默认内存实现；部署 Redis 后可替换为 Redis 版本。
 */
@Service
public class TokenBlacklistService {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        // 默认 24h 后自然过期，可随 token 有效期调整
        blacklist.put(token, Instant.now().getEpochSecond() + 86400L);
    }

    public boolean isBlocked(String token) {
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < Instant.now().getEpochSecond()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
