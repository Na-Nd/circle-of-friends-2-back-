package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.ROLE.ROLE;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.utils.JwtUtil;
import ru.nand.registryservice.entities.DTO.LoginDTO;
import ru.nand.registryservice.entities.DTO.RegisterDTO;
import ru.nand.registryservice.entities.DTO.ResponseDTO;
import ru.nand.registryservice.utils.EncryptionUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaUserAuthListener {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;

    @KafkaListener(topics = "user-registration-topic", groupId = "registry-group")
    public void handleRegistration(String message) {

        RegisterDTO registerDTO;
        try{
            registerDTO = new ObjectMapper().readValue(message, RegisterDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации сообщения регистрации: {}", e.getMessage());
            return;
        }

        log.info("Получено сообщение о регистрации: {}", registerDTO);

        String decryptedPassword = EncryptionUtil.decrypt(registerDTO.getPassword());
        registerDTO.setPassword(decryptedPassword);

        User existingUser = userService.findByUsername(registerDTO.getUsername());

        if(!registerDTO.isEmailVerified()){
            sendResponse("user-registration-response-topic", registerDTO.getRequestId(), "Ошибка: email не подтвержден.");
            return;
        }

        if (existingUser != null) {
            sendResponse("user-registration-response-topic", registerDTO.getRequestId(), "Ошибка: пользователь уже существует.");
            return;
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(ROLE.ROLE_USER);
        user.setRegistrationDate(LocalDateTime.now());
        userService.save(user);

        String token = jwtUtil.generateToken(user);
        sendResponse("user-registration-response-topic", registerDTO.getRequestId(), token);
    }

    @KafkaListener(topics = "user-login-topic", groupId = "registry-group")
    public void handleLogin(String message) {

        //LoginDTO loginDTO = objectMapper.readValue(message, LoginDTO.class);
        LoginDTO loginDTO;
        try{
            loginDTO = new ObjectMapper().readValue(message, LoginDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Ошибка десериализации сообщения логина: {}", e.getMessage());
            return;
        }
        log.info("Получено сообщение о логине: {}", loginDTO);

        String decryptedPassword = EncryptionUtil.decrypt(loginDTO.getPassword());
        loginDTO.setPassword(decryptedPassword);

        User user = userService.findByUsername(loginDTO.getUsername());
        if (user == null) {
            sendResponse("user-login-response-topic", loginDTO.getRequestId(), "Ошибка: пользователь не найден.");
            return;
        }
        if (user.getIsBlocked()) {
            sendResponse("user-login-response-topic", loginDTO.getRequestId(), "Ошибка: пользователь заблокирован.");
            return;
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            sendResponse("user-login-response-topic", loginDTO.getRequestId(), "Ошибка: неверные учетные данные.");
            return;
        }

        String token = jwtUtil.generateToken(user);
        sendResponse("user-login-response-topic", loginDTO.getRequestId(), token);
    }


    private void sendResponse(String topic, String requestId, String message) {
        ResponseDTO responseDTO = new ResponseDTO(requestId, message);
        //String jsonResponse = objectMapper.writeValueAsString(responseDTO); // Сериализация ответа в json

        String jsonResponse;
        try{
            jsonResponse = new ObjectMapper().writeValueAsString(responseDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации в JSON: {}", e.getMessage());
            return;
        }

        kafkaTemplate.send(topic, jsonResponse);
        log.info("Ответ отправлен в топик {}: {}", topic, jsonResponse);
    }

}
