package com.studyroom.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT工具类
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire}")
    private Long refreshTokenExpire;

    /**
     * 生成访问Token
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @return JWT令牌
     */
    public String generateAccessToken(Long userId, String openid) {
        return generateToken(userId, openid, accessTokenExpire);
    }

    /**
     * 生成刷新Token
     * @param userId 用户ID
     * @param openid 微信OpenID
     * @return JWT令牌
     */
    public String generateRefreshToken(Long userId, String openid) {
        return generateToken(userId, openid, refreshTokenExpire);
    }

    /**
     * 生成Token
     */
    private String generateToken(Long userId, String openid, Long expireTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        SecretKey key = getSecretKey();

        return Jwts.builder()
                .setSubject(openid)
                .claim("userId", userId)
                .claim("openid", openid)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = getSecretKey();
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取OpenID
     * @param token JWT令牌
     * @return OpenID
     */
    public String getOpenidFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("openid", String.class);
    }

    /**
     * 解析Token
     */
    private Claims parseToken(String token) {
        SecretKey key = getSecretKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取密钥
     */
    private SecretKey getSecretKey() {
        byte[] encodedKey = Base64.getDecoder().decode(secret);
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "HmacSHA256");
    }
}
