package ru.nand.authservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.nand.authservice.entities.DTO.LoginDTO;
import ru.nand.authservice.entities.DTO.RegisterDTO;
import ru.nand.authservice.entities.DTO.TokenResponse;
import ru.nand.authservice.services.AuthService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /// Регистрация
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterDTO userDTO, BindingResult bindingResult) {
        return authService.registerUser(userDTO, bindingResult);
    }

    /// Подтверждение почты для регистрации
    @PostMapping("/verify-email")
    public ResponseEntity<TokenResponse> verifyAndRegisterUser(@RequestParam String email, @RequestParam String code) {
        return authService.verifyAndRegisterUser(email, code);
    }

    /// Логин
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginDTO loginDTO, BindingResult bindingResult) {
        return authService.loginUser(loginDTO, bindingResult);
    }

    /// Выход
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization") String authHeader) {
        return authService.logout(authHeader);
    }
}