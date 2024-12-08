package ru.nand.registryservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.entities.ROLE.ROLE;
import ru.nand.registryservice.utils.JwtUtil;
import ru.nand.sharedthings.DTO.LoginDTO;
import ru.nand.sharedthings.DTO.RegisterDTO;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaUserAuthListener {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "user-registration-topic", groupId = "registry-group")
    public void listenUserRegistration(RegisterDTO myUserDTO){

        if (userService.findByUsername(myUserDTO.getUsername()) != null){
            kafkaTemplate.send("user-registration-response-topic", "Ошибка валидации: пользователь с таким именем уже существует");
            return;
        }

        if(userService.findByEmail(myUserDTO.getEmail()) != null){
            kafkaTemplate.send("user-registration-response-topic", "Ошибка валидации: пользователь с такой почтой уже существует");
            return;
        }

        User user = new User(myUserDTO.getUsername(), myUserDTO.getEmail(),
                passwordEncoder.encode(myUserDTO.getPassword()),
                ROLE.ROLE_USER, false, LocalDateTime.now()
        );

        user.setRegistrationDate(LocalDateTime.now());

        userService.save(user);

        String token = jwtUtil.generateToken(user);

        kafkaTemplate.send("user-registration-response-topic", token);
        log.info("JWT токен отправлен в Kafka для пользователя: {}", user.getUsername());
    }

    @KafkaListener(topics = "user-login-topic", groupId = "registry-group")
    public void handleLogin(LoginDTO loginDTO){
        log.info("Принято ДТО пользователя: {}", loginDTO.getUsername());

        User user = userService.findByUsername(loginDTO.getUsername());

        // Если такого пользователя нет
        if(user == null){
            kafkaTemplate.send("user-login-response-topic", "Ошибка валидации: такого пользователя: " + loginDTO.getUsername() + " нет");
            return;
        }

        // Если не заблокирован
        if(user.isAccountNonLocked()){
            // Если пароли совпали
            if(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
                String token = jwtUtil.generateToken(user);
                kafkaTemplate.send("user-login-response-topic", token);
            } else {
                kafkaTemplate.send("user-login-response-topic", "Ошибка валидации: пароль неверный");
            }
        } else {
            kafkaTemplate.send("user-login-response-topic", "Ошибка валидации: пользователь " + loginDTO.getUsername() + " заблокирован");
        }
    }
}