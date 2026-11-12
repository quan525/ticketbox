package com.example.ticketbox.auth.service;

import com.example.ticketbox.auth.dto.response.AuthResponse;
import com.example.ticketbox.security.JwtService;
import com.example.ticketbox.security.UserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class RefreshSessions {
    public static final String COOKIE = "ticketbox_refresh";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String KEY_PREFIX = "ticketbox:refresh:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> ROTATE = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        local ttl = redis.call('PTTL', KEYS[1])
        if ttl <= 0 then return 0 end
        redis.call('SET', KEYS[1], ARGV[2], 'PX', ttl)
        return ttl
        """, Long.class);
    private static final DefaultRedisScript<Long> REVOKE = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        return redis.call('DEL', KEYS[1])
        """, Long.class);

    private final StringRedisTemplate redis;
    private final JwtService jwt;
    private final UserDetailsService users;
    private final Duration lifetime;
    private final boolean secureCookie;

    public RefreshSessions(StringRedisTemplate redis, JwtService jwt, UserDetailsService users,
            @Value("${auth.refresh.ttl:14d}") Duration lifetime,
            @Value("${auth.refresh.secure-cookie:true}") boolean secureCookie) {
        if (lifetime.toMillis() < 1000) throw new IllegalArgumentException("Refresh token lifetime must be at least one second");
        this.redis = redis;
        this.jwt = jwt;
        this.users = users;
        this.lifetime = lifetime;
        this.secureCookie = secureCookie;
    }

    public ResponseEntity<AuthResponse> start(AuthResponse access) {
        String username = jwt.extractUsername(access.getToken());
        String sessionId = randomPart();
        String token = sessionId + "." + randomPart();
        try {
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + sessionId, stored(token, username), lifetime))) {
                throw unavailable();
            }
        } catch (DataAccessException failure) {
            throw unavailable();
        }
        return response(access, token, lifetime);
    }

    public ResponseEntity<AuthResponse> refresh(String token, String username) {
        String key = key(token);
        if (key == null) throw expired();
        String replacement = token.substring(0, token.indexOf('.') + 1) + randomPart();
        Long remaining;
        try {
            remaining = redis.execute(ROTATE, List.of(key), stored(token, username), stored(replacement, username));
        } catch (DataAccessException failure) {
            throw unavailable();
        }
        if (remaining == null || remaining <= 0) throw expired();
        try {
            users.loadUserByUsername(username);
        } catch (UsernameNotFoundException failure) {
            throw expired();
        }
        return response(new AuthResponse(jwt.generateToken(username)), replacement, Duration.ofMillis(remaining));
    }

    public ResponseEntity<Void> logout(String token, String username) {
        String key = key(token);
        if (key != null) {
            try {
                Long revoked = redis.execute(REVOKE, List.of(key), stored(token, username));
                if (revoked == null || revoked == 0) return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
            } catch (DataAccessException failure) {
                throw unavailable();
            }
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseEntity<AuthResponse> response(AuthResponse access, String token, Duration ttl) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(token, ttl).toString()).body(access);
    }

    private ResponseCookie cookie(String token, Duration ttl) {
        return ResponseCookie.from(COOKIE, token).httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path(COOKIE_PATH).maxAge(ttl).build();
    }

    private String key(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}\\.[A-Za-z0-9_-]{43}")) return null;
        return KEY_PREFIX + token.substring(0, 43);
    }

    private String randomPart() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String stored(String token, String username) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8))) + ":" + username;
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    private ResponseStatusException expired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again.");
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Session storage is temporarily unavailable.");
    }
}
