package ru.nand.accountuserservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${service.jwt.secret}")
    private String serviceSecretKey;

    // Создать ключ на основе массива байт
    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private Key getInterServiceSigningKey(){
        return Keys.hmacShaKeyFor(serviceSecretKey.getBytes());
    }

    // Извлечь имя
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    // Извлечь роль
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // Извлечь почту
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // Извлечь конкретные данные
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws RuntimeException {
        final Claims claims = extractAllClaims(token);
        if(claims == null){
            throw new RuntimeException("Не получилось разобрать токен");
        }
        return claimsResolver.apply(claims);
    }

    // Извлечь дату истечения
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Получить все данные из токена
    private Claims extractAllClaims(String token) {
        try{
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e){
            log.warn("Не удалось извлечь данные из токена: {}", e.getMessage());
            return null;
        }
    }

    // Валидация токена
    public boolean validateToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e){
            log.error("Ошибка валидации токена: {}", e.getMessage());
            return false;
        }
    }

    // Токен скоро истекает
    public boolean isTokenExpiringSoon(String token) {
        Date expirationDate = extractExpiration(token);
        long timeLeft = expirationDate.getTime() - System.currentTimeMillis();
        return timeLeft <= 240 * 1000; // Проверка, осталось меньше 5 минут
    }

    // Отсечь Bearer_
    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if(bearer != null && bearer.startsWith("Bearer ")){
            return bearer.substring(7);
        }
        return null;
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

    // Генерация пользовательского токена
    public String generateToken(String username, String role, String email) {
        Map<String, Object> claims = new HashMap<>();

        //claims.put("role", user.getRole().name());
        claims.put("role", role);
        claims.put("email", email);
        return createToken(claims, username);
    }

    // Создание пользовательского токена
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
}
