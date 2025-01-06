package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.services.UserService;
import ru.nand.registryservice.utils.JwtUtil;

@Slf4j
@RestController
public class TokenRefreshController {
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public TokenRefreshController(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/api/auth/refresh-token")
    public ResponseEntity<String> refreshToken(
            @RequestHeader("Authorization") String expiredToken) {
        try {
            if (expiredToken != null && expiredToken.startsWith("Bearer ")) {
                expiredToken = expiredToken.substring(7);
            }

            String username = jwtUtil.extractUsername(expiredToken);
            User user = userService.findByUsername(username);

            if (user.getIsBlocked()) {
                return ResponseEntity.status(403).body("Пользователь заблокирован");
            }

            String newToken = jwtUtil.generateToken(user);
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            log.error("Ошибка обновления токена", e);
            return ResponseEntity.status(401).body("Недействительный токен");
        }
    }
}
