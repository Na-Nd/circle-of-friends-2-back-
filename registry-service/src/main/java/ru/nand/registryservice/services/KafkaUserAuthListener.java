package ru.nand.registryservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.ROLE.ROLE;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.utils.JwtUtil;
import ru.nand.sharedthings.DTO.LoginDTO;
import ru.nand.sharedthings.DTO.RegisterDTO;
import ru.nand.sharedthings.DTO.ResponseDTO;
import ru.nand.sharedthings.utils.EncryptionUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaUserAuthListener {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;

    @KafkaListener(topics = "user-registration-topic", groupId = "registry-group")
    public void handleRegistration(RegisterDTO registerDTO) {
        log.info("Получено сообщение о регистрации: {}", registerDTO);

        String decryptedPassword = EncryptionUtil.decrypt(registerDTO.getPassword());
        registerDTO.setPassword(decryptedPassword);

        User existingUser = userService.findByUsername(registerDTO.getUsername());

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
    public void handleLogin(LoginDTO loginDTO) {
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
        System.out.println("Генерирую токен: " + token + " Для пользователя " + user.getUsername() + " и отправляю в брокер");
        sendResponse("user-login-response-topic", loginDTO.getRequestId(), token);
    }


    private void sendResponse(String topic, String requestId, String message) {
        ResponseDTO responseDTO = new ResponseDTO(requestId, message);
        kafkaTemplate.send(topic, responseDTO);
        log.info("Ответ отправлен в топик {}: {}", topic, responseDTO);
    }

}
