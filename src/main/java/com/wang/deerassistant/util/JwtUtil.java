package com.wang.deerassistant.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.wang.deerassistant.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final Algorithm algorithm;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.algorithm = Algorithm.HMAC256(jwtProperties.getSecret());
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);

        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expire)
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm).build().verify(token);
    }

    public Long getUserId(String token) {
        DecodedJWT jwt = verify(token);
        return Long.valueOf(jwt.getSubject());
    }

    public String getUsername(String token) {
        return verify(token).getClaim("username").asString();
    }
}
