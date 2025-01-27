package ru.nand.registryservice.utils;

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

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.core.userdetails.User;

@Slf4j
@Component
public class InterServiceJwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public InterServiceJwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // TODO clear sout
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("Запрос попал в фильтр");
        try{
            String interServiceJwt = jwtUtil.resolveInterServiceToken(request);

            if(interServiceJwt != null){
                if(jwtUtil.validateInterServiceJwt(interServiceJwt)){
                    System.out.println("Межсервисный токен валиден");
                    String serviceName = jwtUtil.extractServiceName(interServiceJwt);
                    String role = jwtUtil.extractRoleFromInterServiceJwt(interServiceJwt);
                    System.out.println(role);

                    if(serviceName != null && SecurityContextHolder.getContext().getAuthentication() == null){
                        UserDetails userDetails = new User(
                                serviceName,
                                "",
                                Collections.singleton(new SimpleGrantedAuthority(role))
                        );

                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                        log.info("Аутентификация сервиса: {}, Роль: {}", serviceName, role);
                    }
                } else {
                    System.out.println("Межсервисный токен невалиден");
                    log.error("Токен не прошел валидацию");
                }
            }
        } catch (Exception e){
            log.error("Ошибка InterServiceJwtRequestFilter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
