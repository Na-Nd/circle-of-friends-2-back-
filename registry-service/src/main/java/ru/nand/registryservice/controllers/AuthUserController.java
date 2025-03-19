package ru.nand.registryservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.AuthService.LoginDTO;
import ru.nand.registryservice.entities.DTO.AuthService.RegisterDTO;
import ru.nand.registryservice.entities.DTO.AuthService.TokenResponse;
import ru.nand.registryservice.entities.UserSession;
import ru.nand.registryservice.services.UserService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthUserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthUserController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> registerUser(@RequestBody String message) {
        try {
            log.info("Пришел запрос на регистрацию от auth-service");
            RegisterDTO registerDTO = objectMapper.readValue(message, RegisterDTO.class);

            // Регистрация и создание сессии
            UserSession session = userService.registerUser(registerDTO);

            return ResponseEntity.status(200).body(new TokenResponse(session.getAccessToken(), session.getRefreshToken()));
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(new TokenResponse(null, null, "Ошибка при регистрации: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@RequestBody String message) {
        try {
            log.info("Пришел запрос на логин от auth-service");
            LoginDTO loginDTO = objectMapper.readValue(message, LoginDTO.class);

            // Аутентификация и создание сессии
            UserSession session = userService.authenticateUser(loginDTO.getUsername(), loginDTO.getPassword());

            return ResponseEntity.ok(new TokenResponse(session.getAccessToken(), session.getRefreshToken()));
        } catch (Exception e) {
            log.error("Ошибка при аутентификации пользователя: {}", e.getMessage());

            return ResponseEntity.status(401).body(new TokenResponse(null, null, "Ошибка при аутентификации: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String accessToken){
        try{
            log.info("Пришел запрос на логаут от auth-service");
            userService.logoutUser(accessToken);

            return ResponseEntity.status(200).body("Успешный выход из системы");
        } catch (Exception e){
            log.error("Ошибка при логауте пользователя: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при выходе: " + e.getMessage());
        }
    }
}
