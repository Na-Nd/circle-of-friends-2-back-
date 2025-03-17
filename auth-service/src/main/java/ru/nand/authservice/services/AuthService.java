package ru.nand.authservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.client.RestTemplate;
import ru.nand.authservice.entities.DTO.LoginDTO;
import ru.nand.authservice.entities.DTO.RegisterDTO;
import ru.nand.authservice.entities.DTO.TokenResponse;
import ru.nand.authservice.utils.AuthUtil;
import ru.nand.authservice.utils.JwtUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final MailSenderService mailSenderService;
    private final AuthUtil authUtil;

    @Value("${registry.service.url}")
    private String registryServiceUrl;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    /// Регистрация
    public ResponseEntity<?> registerUser(RegisterDTO registerDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return authUtil.handleValidationErrors(bindingResult);
        }

        // Генерация кода
        String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);

        // Сохранение временных данных в Redis
        redisService.saveWithExpiration("verification_code:" + registerDTO.getEmail(), verificationCode, 5, TimeUnit.MINUTES);
        redisService.saveWithExpiration("pending_registration:" + registerDTO.getEmail(), registerDTO, 5, TimeUnit.MINUTES);

        // Отправка кода
        mailSenderService.sendVerificationMail(registerDTO, verificationCode);

        return ResponseEntity.status(200).body("Код верификации отправлен. Проверьте свою почту.");
    }

    /// Подтверждение почты для регистрации
    public ResponseEntity<TokenResponse> verifyAndRegisterUser(String email, String code) {
        String savedCode = (String) redisService.get("verification_code:" + email);
        RegisterDTO userDTO = redisService.getRegisterDTO("pending_registration:" + email);

        if (savedCode == null || userDTO == null) {
            return ResponseEntity.status(400).body(new TokenResponse(null, null, "Код верификации истек или неверен. Попробуйте зарегистрироваться снова."));
        }

        if (!savedCode.equals(code)) {
            return ResponseEntity.status(400).body(new TokenResponse(null, null, "Неверный код верификации."));
        }

        // Удаление временных данных из Redis
        redisService.delete("verification_code:" + email);
        redisService.delete("pending_registration:" + email);

        // Установка флага верификации email
        userDTO.setEmailVerified(true);

        // Отправка запроса на регистрацию пользователя
        try {
            TokenResponse response = registerUserInRegistryService(userDTO);
            return ResponseEntity.status(200).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(new TokenResponse(null, null, e.getMessage()));
        }
    }

    /// Запрос на регистрацию пользователя
    private TokenResponse registerUserInRegistryService(RegisterDTO registerDTO) {
        String url = registryServiceUrl + "/api/auth/register";

        // Сериализация DTO в JSON
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(registerDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации RegisterDTO: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        log.debug("Отправка запроса на регистрацию пользователя: {}", registerDTO);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    TokenResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Неуспешный ответ от registry-service при регистрации: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при отправке запроса на регистрацию: {}", e.getMessage());
            throw new RuntimeException("Ошибка при отправке запроса на регистрацию: " + e.getMessage());
        }
    }

    /// Логин
    public ResponseEntity<TokenResponse> loginUser(LoginDTO loginDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return authUtil.handleValidationErrors(bindingResult);
        }

        // Отправка запроса на логин
        try {
            TokenResponse response = loginUserInRegistryService(loginDTO);
            return ResponseEntity.status(200).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(new TokenResponse(null, null, e.getMessage()));
        }
    }

    /// Запрос на логин
    private TokenResponse loginUserInRegistryService(LoginDTO loginDTO) {
        String url = registryServiceUrl + "/api/auth/login";

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(loginDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации LoginDTO: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        log.debug("Отправка запроса на аутентификацию пользователя: {}", loginDTO);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    TokenResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Неуспешный ответ от registry-service при логине: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при отправке запроса на аутентификацию: {}", e.getMessage());
            throw new RuntimeException("Ошибка при отправке запроса на аутентификацию: " + e.getMessage());
        }
    }

    /// Выход
    public ResponseEntity<?> logout(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);

                // Отправка запроса на выход
                logoutInRegistryService(accessToken);

                return ResponseEntity.status(200).body("Вы успешно вышли из системы");
            } else {
                return ResponseEntity.status(400).body("Неверный формат токена");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при выходе: " + e.getMessage());
        }
    }

    /// Запрос на выход
    private void logoutInRegistryService(String accessToken) {
        String url = registryServiceUrl + "/api/auth/logout?accessToken=" + accessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Отправка запроса на логаут в registry-service");

        try {
            // Отчищаем КБ
            SecurityContextHolder.clearContext();

            // Отправляем запрос на логаут в registry-service
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при выходе из системы: {}", e.getMessage());
            throw new RuntimeException("Ошибка при выходе из системы: " + e.getMessage());
        }
    }
}