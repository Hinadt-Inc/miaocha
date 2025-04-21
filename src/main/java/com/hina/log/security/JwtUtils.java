package com.hina.log.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:defaultSecretKeyNeedsToBeAtLeast32CharsLong}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration:86400000}")
    private int refreshTokenExpirationMs;

    // 令牌提前过期的缓冲时间（秒）
    private static final int TOKEN_EXPIRATION_BUFFER_SECONDS = 30;

    /**
     * 生成JWT token
     *
     * @param uid 用户ID
     * @return JWT token
     */
    public String generateToken(String uid) {
        return buildToken(uid, new HashMap<>(), jwtExpirationMs);
    }

    /**
     * 生成刷新token
     *
     * @param uid 用户ID
     * @return refresh token
     */
    public String generateRefreshToken(String uid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(uid, claims, refreshTokenExpirationMs);
    }

    /**
     * 从refresh token生成新的access token
     *
     * @param refreshToken 刷新token
     * @return 新的token，如果刷新token无效则返回null
     * @throws ExpiredJwtException 如果刷新token已过期
     */
    public String generateTokenFromRefreshToken(String refreshToken) throws ExpiredJwtException {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            // 验证是否为刷新token
            if (!"refresh".equals(claims.get("type"))) {
                return null;
            }

            String uid = claims.getSubject();
            return generateToken(uid);
        } catch (ExpiredJwtException e) {
            // 刷新token过期，向上抛出这个异常，以便调用者区分处理
            log.error("Refresh token has expired: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while generating token from refresh token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取token的过期时间
     *
     * @param token JWT token
     * @return 过期时间的毫秒数
     */
    public long getExpirationFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            log.error("Error while getting expiration from token: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查令牌是否即将过期
     * 
     * @param token JWT token
     * @return 是否即将过期
     */
    public boolean isTokenNearExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            // 计算还有多少毫秒过期
            long timeToExpire = expiration.getTime() - now.getTime();

            // 如果小于缓冲时间，则认为即将过期
            return timeToExpire < (TOKEN_EXPIRATION_BUFFER_SECONDS * 1000);
        } catch (Exception e) {
            return true; // 如果解析出错，也视为即将过期
        }
    }

    /**
     * 构建token
     *
     * @param subject      token主题
     * @param claims       token声明
     * @param expirationMs 过期时间（毫秒）
     * @return JWT token
     */
    private String buildToken(String subject, Map<String, Object> claims, int expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JwtBuilder jwtBuilder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        // 添加claims
        claims.forEach(jwtBuilder::claim);

        return jwtBuilder.compact();
    }

    /**
     * 从token中获取uid
     *
     * @param token JWT token
     * @return 用户ID
     */
    public String getUidFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);

            // 如果token即将过期，也视为无效
            if (isTokenNearExpiration(token)) {
                log.warn("JWT token is about to expire");
                return false;
            }

            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}