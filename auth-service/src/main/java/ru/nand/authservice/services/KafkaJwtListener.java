package ru.nand.authservice.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Getter
@Slf4j
@Service
public class KafkaJwtListener {

    private volatile String jwtToken;  // Чтобы потоки работали с актуальным значением токена

    @KafkaListener(topics = "user-registration-response-topic", groupId = "auth-group")
    public void listenJwtToken(String token){
        // Токен - это ответ от сервиса реестров (тут может быть токен или ошибки всякие)
        this.jwtToken = token;
        log.info("Получен JWT после регистрации из Kafka: {}", token);
    }

    @KafkaListener(topics = "user-login-response-topic", groupId = "auth-group")
    public void listenLoginJwtToken(String token){
        this.jwtToken = token;
        log.info("Получен JWT после логина из Kafka: {}", token);
    }

    // Сброс токена перед новым запросом
    public void resetToken() {
        this.jwtToken = null;
        log.info("Сброшен токен перед новым запросом.");
    }

}
