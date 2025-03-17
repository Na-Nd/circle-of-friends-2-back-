package ru.nand.authservice.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${service.jwt.secret}")
    private String serviceSecretKey;

    private Key getInterServiceSigningKey(){
        return Keys.hmacShaKeyFor(serviceSecretKey.getBytes());
    }

    // Генерация межсервисного JWT
    public String generateInterServiceJwt(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("service_role", "ROLE_SERVICE");

        return createInterServiceToken(claims);
    }

    // Создание межсервисного JWT
    private String createInterServiceToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(serviceName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getInterServiceSigningKey())
                .compact();
    }

}
