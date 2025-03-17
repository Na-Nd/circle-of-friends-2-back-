package ru.nand.registryservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.services.UserService;

import java.security.Key;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${accountuserservice.jwt.secret}")
    private String serviceSecret;

    @Value("${jwt.access.jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.jwt.expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private Key getServiceKey(){
        return Keys.hmacShaKeyFor(serviceSecret.getBytes());
    }

    public String extractServiceName(String token){
        return extractClaimFromInterServiceToken(token, Claims::getSubject);
    }

    public <T> T extractClaimFromInterServiceToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaimsFromInterServiceToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims extractAllClaimsFromInterServiceToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getServiceKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /// Генерация access токена
    public String generateAccessToken(User user){
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("token_type", "access");
        claims.put("user_id", user.getId());

        return createToken(claims, user.getUsername(), accessTokenExpiration);
    }

    /// Генерация refresh токена
    public String generateRefreshToken(User user){
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("token_type", "refresh");
        claims.put("user_id", user.getId());

        return createToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    /// Создание токена
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /// Валидация истечения токена
    public boolean validateExpirationToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e){
            log.error("Ошибка валидации истечения токена: {}", e.getMessage());
            return false;
        }
    }

    /// Отсечение Bearer части межсервисного токена
    public String resolveInterServiceToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_NAME);

        if(bearer != null && bearer.startsWith("Bearer ")){
            log.debug("Bearer межсервисоного токена перед отсечением: {}", bearer);
            return bearer.substring(7);
        }
        return null;
    }

    /// Извлечение роли микросервиса из межсервисного токена
    public String extractRoleFromInterServiceJwt(String token){
        return extractClaimFromInterServiceToken(token, claims -> claims.get("service_role", String.class));
    }

    /// Валидация межсервисного токена
    public boolean validateInterServiceJwt(String interServiceJwt) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getServiceKey())
                    .build()
                    .parseClaimsJws(interServiceJwt);

            return true;
        } catch (Exception e){
            log.error("Ошибка валидации токена: {}", e.getMessage());
            return false;
        }
    }
}
