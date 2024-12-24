package ru.nand.registryservice.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.nand.sharedthings.utils.KeyGenerator;

import java.io.IOException;

@Component
public class InterserviceFilter extends OncePerRequestFilter {

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${interservice.secret.key}")
    private String SECRET_KEY;

    @Value("${myplug}")
    private String plug; // Заглушка


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String headerValue = request.getHeader(HEADER_NAME);

        if(headerValue == null || !isValidHeader(headerValue)){
            response.setStatus(403);
            response.getWriter().write("Пустой или невалидный заголовок");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidHeader(String headerValue){
        String generatedKey = KeyGenerator.generateKey(SECRET_KEY, plug);
        return generatedKey.equals(headerValue);
    }
}
