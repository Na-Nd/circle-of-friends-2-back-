package ru.nand.accountuserservice.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.core.userdetails.User;
import ru.nand.accountuserservice.services.TokenRefreshGrpcClient;

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenRefreshGrpcClient tokenRefreshClient;

    @Autowired
    public JwtRequestFilter(JwtUtil jwtUtil, TokenRefreshGrpcClient tokenRefreshClient) {
        this.jwtUtil = jwtUtil;
        this.tokenRefreshClient = tokenRefreshClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = jwtUtil.resolveToken(request);

            if (token != null) {
                // Если токен скоро истечет
                if (jwtUtil.isTokenExpiringSoon(token)) {
                    log.info("Токен скоро истечет, запрашиваем новый токен");
                    String refreshedToken = tokenRefreshClient.refreshToken(token);

                    // Обновляем токен
                    if (refreshedToken != null) {
                        log.info("Токен был успешно обновлен");
                        token = refreshedToken;
                        // Устанавливаем новый токен в заголовок ответа
                        response.setHeader("Authorization", "Bearer " + token);
                    } else {
                        log.warn("Не удалось обновить токен");
                    }
                }
                // Валидация токена и добавление текущего пользователя в КБ
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = new User(
                                username,
                                "",
                                Collections.singleton(new SimpleGrantedAuthority(role))
                        );

                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        log.debug("Пользователь в фильтре: {}, Роль: {}", username, role);
                    }
                } else {
                    log.error("Токен не прошел валидацию");
                }
            }
        } catch (Exception e) {
            log.error("Ошибка JwtRequestFilter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

}
