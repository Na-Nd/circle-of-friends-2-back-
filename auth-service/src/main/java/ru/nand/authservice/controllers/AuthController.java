package ru.nand.authservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.authservice.services.KafkaJwtListener;
import ru.nand.sharedthings.DTO.LoginDTO;
import ru.nand.sharedthings.DTO.RegisterDTO;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaJwtListener kafkaJwtListener;

    @Autowired
    public AuthController(KafkaTemplate<String, Object> kafkaTemplate, KafkaJwtListener kafkaJwtListener) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaJwtListener = kafkaJwtListener;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterDTO userDTO, BindingResult bindingResult)
            throws JsonProcessingException {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        sendToKafka("user-registration-topic", userDTO);
        return waitForToken("Ответ на попытку регистрации пользователя " + userDTO.getUsername() + ": ");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody LoginDTO loginDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return handleValidationErrors(bindingResult);
        }

        sendToKafka("user-login-topic", loginDTO);
        return waitForToken("Ответ на попытку логина пользователя: " + loginDTO.getUsername() + ": ");
    }

    private ResponseEntity<String> handleValidationErrors(BindingResult bindingResult) {
        String errMsg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        log.warn("Ошибка валидации: {}", errMsg);

        return ResponseEntity.badRequest().body("Ошибка валидации: " + errMsg);
    }

    private void sendToKafka(String topic, Object message) {
        kafkaTemplate.send(topic, message);
        log.info("Сообщение отправлено в топик {}: {}", topic, message);
    }

    private ResponseEntity<String> waitForToken(String successMessage) {
        CompletableFuture<String> futureToken = CompletableFuture.supplyAsync(() -> {
            while (kafkaJwtListener.getJwtToken() == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Асинхронная задача прервана.");
                }
            }
            return kafkaJwtListener.getJwtToken();
        });

        try {
            String token = futureToken.get(15, TimeUnit.SECONDS);
            if(token.contains("Ошибка валидации")){
                return ResponseEntity.status(400).body(token);
            }
            return ResponseEntity.ok(successMessage + " " + token);
        } catch (Exception e) {
            log.warn("Ошибка: не удалось получить JWT токен вовремя.");
            return ResponseEntity.status(504).body("Ошибка: не удалось получить JWT токен вовремя.");
        }
    }
}