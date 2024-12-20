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
import ru.nand.sharedthings.utils.KeyGenerator;

import java.util.Base64;


// TODO попробовать делать это дело не с уже истекшим токеном, так как истекший токен не получится разобрать, а с истекающим (например осталась 1 минута до конца срока и тогда делаем запрос на обновление)
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
            @RequestHeader("Authorization") String expiredToken,
            @RequestHeader("X-SECRET-KEY") String secretKey) {
        try {
            if(expiredToken != null && expiredToken.startsWith("Bearer ")) {
                expiredToken = expiredToken.substring(7);
            }
            // Проверяем секретный ключ
            String expectedKey = KeyGenerator.generateKey("myKey", expiredToken);
            System.out.println("expectedKey: " + expectedKey);
            System.out.println("secretKey: " + secretKey);
            System.out.println("expiredToken: " + expiredToken);
            if (!expectedKey.equals(secretKey)) {
                return ResponseEntity.status(403).body("Неверный секретный ключ");
            }

            // Проверяем пользователя
            String username = jwtUtil.extractUsername(expiredToken);
            User user = userService.findByUsername(username);
            if (user.getIsBlocked()) {
                return ResponseEntity.status(403).body("Пользователь заблокирован");
            }

            // Генерация нового токена
            String newToken = jwtUtil.generateToken(user);
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Недействительный токен");
        }
    }
}
