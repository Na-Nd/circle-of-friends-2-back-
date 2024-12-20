package ru.nand.accountuserservice.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.nand.accountuserservice.services.TokenRefreshClient;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.core.userdetails.User;

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenRefreshClient tokenRefreshClient;

    public JwtRequestFilter(JwtUtil jwtUtil, TokenRefreshClient tokenRefreshClient) {
        this.jwtUtil = jwtUtil;
        this.tokenRefreshClient = tokenRefreshClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = jwtUtil.resolveToken(request);

            if (token != null) {
                // Если осталось меньше 1 минуты до истечения токена
                if (jwtUtil.isTokenExpiringSoon(token)) {
                    log.info("Токен скоро истечет, запрашиваем новый токен");
                    String refreshedToken = tokenRefreshClient.refreshToken(token);

                    // Обновляем токен
                    if (refreshedToken != null) {
                        log.info("Токен был успешно обновлен");
                        token = refreshedToken;
                        // Устанавливаем новый токен в заголовок ответа, если это требуется
                        response.setHeader("Authorization", "Bearer " + token);
                    } else {
                        log.warn("Не удалось обновить токен");
                    }
                }

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
                        log.info("Аутентификация пользователя: {}, Роль: {}", username, role);
                    }
                } else {
                    log.warn("Токен не прошел валидацию");
                }
            }
        } catch (Exception e) {
            log.error("Ошибка JwtRequestFilter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

}
