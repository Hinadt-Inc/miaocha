package com.hinadt.miaocha.config.security;

import com.hinadt.miaocha.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT 工具类 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:defaultSecretKeyNeedsToBeAtLeast32CharsLong}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private int jwtExpirationMs;

    @Value("${jwt.refresh-expiration:86400000}")
    private int refreshTokenExpirationMs;

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
     * 生成JWT token（包含用户信息和登录方式）
     *
     * @param user 用户对象
     * @param loginType 登录方式
     * @return JWT token
     */
    public String generateTokenWithUserInfo(User user, String loginType) {
        Map<String, Object> claims = buildUserClaims(user, loginType);
        return buildToken(user.getUid(), claims, jwtExpirationMs);
    }

    /**
     * 生成刷新token（包含用户信息和登录方式）
     *
     * @param user 用户对象
     * @param loginType 登录方式
     * @return refresh token
     */
    public String generateRefreshTokenWithUserInfo(User user, String loginType) {
        Map<String, Object> claims = buildUserClaims(user, loginType);
        claims.put("type", "refresh");
        return buildToken(user.getUid(), claims, refreshTokenExpirationMs);
    }

    /**
     * 获取token的过期时间
     *
     * @param token JWT token
     * @return 过期时间的毫秒数
     */
    public long getExpirationFromToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
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
     * 构建token
     *
     * @param subject token主题
     * @param claims token声明
     * @param expirationMs 过期时间（毫秒）
     * @return JWT token
     */
    private String buildToken(String subject, Map<String, Object> claims, int expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JwtBuilder jwtBuilder =
                Jwts.builder()
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
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        Object userId = claims.get(User.Fields.uid);
        if (userId == null) {
            throw new RuntimeException("JWT token中缺少userId字段");
        }

        return String.valueOf(userId);
    }

    /**
     * 从token中获取用户ID
     *
     * @param token JWT token
     * @return 用户ID
     * @throws RuntimeException 如果token中没有userId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        Object userId = claims.get(User.Fields.id);
        if (userId == null) {
            throw new RuntimeException("JWT token中缺少userId字段");
        }
        return Long.valueOf(userId.toString());
    }

    /**
     * 从token中获取用户邮箱
     *
     * @param token JWT token
     * @return 用户邮箱
     * @throws RuntimeException 如果token中没有email
     */
    public String getEmailFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        String email = (String) claims.get(User.Fields.email);
        if (email == null) {
            throw new RuntimeException("JWT token中缺少email字段");
        }
        return email;
    }

    /**
     * 从token中获取用户昵称
     *
     * @param token JWT token
     * @return 用户昵称，可能为null（nickname不是必需字段）
     */
    public String getNicknameFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        return (String) claims.get(User.Fields.nickname);
    }

    /**
     * 从token中获取用户角色
     *
     * @param token JWT token
     * @return 用户角色
     * @throws RuntimeException 如果token中没有role
     */
    public String getRoleFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        String role = (String) claims.get(User.Fields.role);
        if (role == null) {
            throw new RuntimeException("JWT token中缺少role字段");
        }
        return role;
    }

    /**
     * 从token中获取用户状态
     *
     * @param token JWT token
     * @return 用户状态
     * @throws RuntimeException 如果token中没有status
     */
    public Integer getStatusFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        Integer status = (Integer) claims.get(User.Fields.status);
        if (status == null) {
            throw new RuntimeException("JWT token中缺少status字段");
        }
        return status;
    }

    /**
     * 从token中获取登录方式
     *
     * @param token JWT token
     * @return 登录方式
     */
    public String getLoginTypeFromToken(String token) {
        Claims claims =
                Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
        String loginType = (String) claims.get("loginType");
        return loginType != null ? loginType : "system"; // 默认为系统登录
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT token
     */
    public void validateToken(String token) {
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
    }

    /**
     * 构建用户信息claims
     *
     * @param user 用户对象
     * @param loginType 登录方式
     * @return claims map
     */
    private Map<String, Object> buildUserClaims(User user, String loginType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(User.Fields.id, user.getId());
        claims.put(User.Fields.uid, user.getUid());
        claims.put(User.Fields.nickname, user.getNickname());
        claims.put(User.Fields.email, user.getEmail());
        claims.put(User.Fields.role, user.getRole());
        claims.put(User.Fields.status, user.getStatus());
        claims.put("loginType", loginType);
        return claims;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
