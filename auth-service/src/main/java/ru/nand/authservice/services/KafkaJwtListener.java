package ru.nand.authservice.services;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaJwtListener {
    private volatile String jwtToken; // Чтобы потоки работали с актуальным значением токена

    @KafkaListener(topics = "user-registration-response-topic", groupId = "auth-group")
    public void listenJwtToken(String token){
        this.jwtToken = token;
        log.info("Получен JWT после регистрации из Kafka{}", token);
    }

    @KafkaListener(topics = "user-login-response-topic", groupId = "auth-group")
    public void listenLoginJwtToken(String token){
        this.jwtToken = token;
        log.info("Получен JWT после логина из Kafka: {}", token);
    }

    public String getJwtToken(){
        return jwtToken;
    }
}