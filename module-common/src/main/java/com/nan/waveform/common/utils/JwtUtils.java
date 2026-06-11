package com.nan.waveform.common.utils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * @author nan chao
 * @since 2026/6/11 10:41
 */

@Component
public class JwtUtils {
    // 从 application.yml 中读取密钥
    @Value("${jwt.secret}")
    private String secret;

    // 从 application.yml 中读取过期时间（秒）
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @param username 用户名
     */
    public String createToken(Long userId, String username) {
        Date expireDate = new Date(System.currentTimeMillis() + expiration * 1000);

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        return JWT.create()
                .withHeader(header)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withExpiresAt(expireDate) // 过期时间
                .withIssuedAt(new Date())   // 签发时间
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 解析并验证 Token
     * @param token 令牌
     * @return DecodedJWT 解析后的JWT对象，若验证失败会抛出异常
     */
    public DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token); // 验证不通过会抛出 JWTVerificationException
    }
}
