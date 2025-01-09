package ru.nand.registryservice.utils;

import io.jsonwebtoken.Claims;
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

    @Value("${jwt.expiration}")
    private long expiration;

    private final UserService userService;

    @Autowired
    public JwtUtil(UserService userService) {
        this.userService = userService;
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private Key getServiceKey(){
        return Keys.hmacShaKeyFor(serviceSecret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractServiceName(String token){
        return extractClaimFromInterServiceToken(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
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

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        User user = userService.findByUsername(userDetails.getUsername());

        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        return createToken(claims, userDetails.getUsername());
    }

    // TODO private mb
    public String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e){
            log.warn("Произошла ошибка валидации токена");
            return false;
        }
    }

    public Set<GrantedAuthority> extractAuthorities(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }


    public UsernamePasswordAuthenticationToken getAuthentication(String token, UserDetails userDetails) {
        Set<GrantedAuthority> authorities = extractAuthorities(token);
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String resolveInterServiceToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER_NAME);

        if(bearer != null && bearer.startsWith("Bearer ")){
            log.info("Bearer межсервисоного токена перед отсечением: {}", bearer);
            return bearer.substring(7);
        }
        return null;
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractRoleFromInterServiceJwt(String token){
        return extractClaimFromInterServiceToken(token, claims -> claims.get("service_role", String.class));
    }

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
