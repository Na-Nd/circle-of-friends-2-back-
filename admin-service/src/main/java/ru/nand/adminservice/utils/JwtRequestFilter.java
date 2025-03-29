package ru.nand.adminservice.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.nand.adminservice.services.CheckSessionClient;
import ru.nand.adminservice.services.TokenRefreshGrpcClient;
import ru.nand.adminservice.utils.JwtUtil;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenRefreshGrpcClient tokenRefreshGrpcClient;
    private final CheckSessionClient sessionClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            String token = jwtUtil.resolveToken(request);

            if(token != null){
                // Проверяем, истек ли токен
                if (!jwtUtil.validateExpirationToken(token)) {
                    log.info("Токен истек, запрашиваем новый токен");
                    String refreshedToken = tokenRefreshGrpcClient.refreshToken(token);

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
                // Валидация токена и добавление текущего пользователя в КБ
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
                }
            }
        } catch (Exception e){
            log.error("Ошибка JwtRequestFilter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
