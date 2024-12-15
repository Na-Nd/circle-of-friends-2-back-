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

        // Расшифровываем пароль
        String decryptedPassword = EncryptionUtil.decrypt(registerDTO.getPassword());
        registerDTO.setPassword(decryptedPassword);
        System.out.println(registerDTO);

        User existingUser = userService.findByUsername(registerDTO.getUsername());

        if (existingUser != null) {
            sendResponse("user-registration-response-topic", "Ошибка: пользователь уже существует.");
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
        sendResponse("user-registration-response-topic", token);
    }

    @KafkaListener(topics = "user-login-topic", groupId = "registry-group")
    public void handleLogin(LoginDTO loginDTO) {
        log.info("Получено сообщение о логине: {}", loginDTO);

        // Расшифровываем пароль
        String decryptedPassword = EncryptionUtil.decrypt(loginDTO.getPassword());
        loginDTO.setPassword(decryptedPassword);

        User user = userService.findByUsername(loginDTO.getUsername());
        if(user == null){
            sendResponse("user-login-response-topic", "Ошибка: пользователь не найден.");
            return;
        }
        if(user.getIsBlocked()){
            sendResponse("user-login-response-topic", "Ошибка: пользователь заблокирован.");
            return;
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            sendResponse("user-login-response-topic", "Ошибка: неверные учетные данные.");
            return;
        }
        
        String token = jwtUtil.generateToken(user);
        sendResponse("user-login-response-topic", token);
    }

    private void sendResponse(String topic, String message) {
        kafkaTemplate.send(topic, message);
        log.info("Ответ отправлен в топик {}: {}", topic, message);
    }
}
