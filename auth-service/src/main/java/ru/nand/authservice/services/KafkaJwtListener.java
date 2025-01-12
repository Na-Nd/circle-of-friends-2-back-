package ru.nand.authservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entities.DTO.ResponseDTO;

import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
@Service
public class KafkaJwtListener {

    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();

    @KafkaListener(topics = "user-registration-response-topic", groupId = "auth-group")
    public void listenRegistrationJwtToken(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseDTO responseDTO = objectMapper.readValue(message, ResponseDTO.class);
        tokenStore.put(responseDTO.getRequestId(), responseDTO.getToken());
        log.info("Получен JWT после регистрации для requestId {}: {}", responseDTO.getRequestId(), responseDTO.getToken());
    }

    @KafkaListener(topics = "user-login-response-topic", groupId = "auth-group")
    public void listenLoginJwtToken(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseDTO responseDTO = objectMapper.readValue(message, ResponseDTO.class);

        tokenStore.put(responseDTO.getRequestId(), responseDTO.getToken());
        log.info("Получен JWT после логина для requestId {}: {}", responseDTO.getRequestId(), responseDTO.getToken());
    }

    public String getJwtToken(String requestId) {
        return tokenStore.get(requestId);
    }

    public void resetToken(String requestId) {
        tokenStore.remove(requestId);
    }

}
