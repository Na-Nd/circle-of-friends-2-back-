package ru.nand.accountuserservice.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import ru.nand.accountuserservice.services.AccountService;
import ru.nand.accountuserservice.services.CheckSessionClient;
import ru.nand.accountuserservice.services.TokenRefreshGrpcClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenRefreshGrpcClient tokenRefreshClient;
    private final CheckSessionClient sessionClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = jwtUtil.resolveToken(request);

            if (token != null) {
                // Проверяем, истек ли токен
                if (!jwtUtil.validateExpirationToken(token)) {
                    log.info("Токен истек, запрашиваем новый токен");
                    String refreshedToken = tokenRefreshClient.refreshToken(token);

                    // Если токен успешно обновлен
                    if (refreshedToken != null) {
                        log.info("Токен был успешно обновлен");
                        token = refreshedToken;
                        // Устанавливаем новый токен в заголовок ответа
                        response.setHeader("Authorization", "Bearer " + token);
                    } else {
                        log.warn("Не удалось обновить токен");
                        // Если токен не удалось обновить, завершаем запрос с ошибкой
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Токен истек и не может быть обновлен");
                        return;
                    }
                }

                // Валидация токена и добавление текущего пользователя в контекст безопасности
                if (jwtUtil.validateToken(token) && sessionClient.isSessionActive(token)) {
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
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Недействительный токен");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Ошибка JwtRequestFilter: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка обработки токена");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
