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

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = jwtUtil.resolveToken(request);

            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Создаем UserDetails из токена, чтобы не дергать сервис реестров при каждом запросе
                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                            username,
                            "",
                            Collections.singleton(new SimpleGrantedAuthority(role))
                    );

                    // Установим аутентификацию в КБ
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    // Логируем информацию о пользователе
                    log.info("Аутентификация пользователя: {}, Роль: {}", username, role);
                } else {
                    log.warn("Username пустой или контекст уже занят. Username: {}, Role: {}", username, role);
                }
            } else {
                log.warn("Токен пустой или не прошел валидацию");
            }
        } catch (Exception e) {
            log.error("Ошибка JwtRequestFilter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

}
