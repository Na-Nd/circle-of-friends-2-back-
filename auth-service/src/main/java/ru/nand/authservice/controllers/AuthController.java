package ru.nand.authservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.nand.authservice.entities.DTO.LoginDTO;
import ru.nand.authservice.entities.DTO.NotificationDTO;
import ru.nand.authservice.entities.DTO.RegisterDTO;
import ru.nand.authservice.services.KafkaJwtListener;
import ru.nand.authservice.services.MailSenderService;
import ru.nand.authservice.utils.EncryptionUtil;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MailSenderService mailSenderService;
    private final KafkaJwtListener kafkaJwtListener;

    private final Map<String, RegisterDTO> pendingRegistrations = new ConcurrentHashMap<>();
    private final Map<String, String> emailVerificationCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> verificationTimestamps = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public Mono<String> registerUser(@RequestBody RegisterDTO userDTO) {
        // Генерация и отправка кода подтверждения
        String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);
        emailVerificationCodes.put(userDTO.getEmail(), verificationCode);
        verificationTimestamps.put(userDTO.getEmail(), System.currentTimeMillis());

        // Сохраняем данные пользователя в мапу по ключу его почты
        pendingRegistrations.put(userDTO.getEmail(), userDTO);

        mailSenderService.sendMail(
                userDTO.getEmail(),
                "Код верификации",
                "Здравствуйте, " + userDTO.getUsername() + ", ваш код верификации: " + verificationCode
        );

        log.info("Код подтверждения отправлен на email: {}", userDTO.getEmail());
        return Mono.just("Код верификации отправлен. Проверьте свою почту.");
    }

    // Сюда должен быть редирект после registerUser()
    @PostMapping("/verify-email")
    public Mono<Object> verifyAndRegisterUser(@RequestParam String email, @RequestParam String code){
        String savedCode = emailVerificationCodes.get(email);
        Long timestamp = verificationTimestamps.get(email);
        RegisterDTO userDTO = pendingRegistrations.get(email);

        if (savedCode == null || timestamp == null || userDTO == null) {
            //return Mono.just("Код верификации истек или неверен. Попробуйте зарегистрироватся снова.");
            return Mono.just(ResponseEntity.status(400)
                    .body("Код верификации истек или неверен. Попробуйте зарегистрироватся снова."));
        }

        if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) { // 5 минут
            emailVerificationCodes.remove(email);
            verificationTimestamps.remove(email);
            pendingRegistrations.remove(email);
            //return Mono.just("Код верификации истек. Попробуйте зарегистриороваться снова.");
            return Mono.just(ResponseEntity.status(400)
                    .body("Код верификации истек. Попробуйте зарегистрироваться снова."));
        }

        if (!savedCode.equals(code)) {
            //return Mono.just("Неверный код верификации.");
            return Mono.just(ResponseEntity.status(400)
                    .body("Неверный код верификации."));
        }

        emailVerificationCodes.remove(email);
        verificationTimestamps.remove(email);

        log.info("Email {} успешно подтвержден", email);

        userDTO.setEmailVerified(true);
        userDTO.setPassword(EncryptionUtil.encrypt(userDTO.getPassword())); // Шифруем пароль перед отправкой в кафку
        String requestId = UUID.randomUUID().toString();
        userDTO.setRequestId(requestId);

        log.info("Отправка сообщения регистрации в топик user-registration-topic: {}", userDTO);

        // Сериализация данных регистрирующегося пользователя
        String registrationMessage;
        try{
            registrationMessage = new ObjectMapper().writeValueAsString(userDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации данных при регистрации пользователя {}: {}", userDTO.getUsername(), e.getMessage());
            //return Mono.just("Внутренняя ошибка сервера");
            return Mono.just(ResponseEntity.status(500)
                    .body("Внутренняя ошибка сервера"));
        }

        log.info("Отправка уведомления для пользователя {}", userDTO.getEmail());

        NotificationDTO notificationDTO = new NotificationDTO(userDTO.getEmail(), userDTO.getUsername() + ", ваш аккаунт успешно зарегистрирован");
        String notificationMessage;
        // Сериализация уведомления
        try {
            notificationMessage = new ObjectMapper().writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e){
            log.error("Ошибка сериализации уведомления: {}", e.getMessage());
            //return Mono.just("Внутренняя ошибка сервера");
            return Mono.just(ResponseEntity.status(500)
                    .body("Внутренняя ошибка сервера"));
        }

        kafkaTemplate.send("user-notifications-topic", notificationMessage);

        // Сброс токена для нового запроса
        kafkaJwtListener.resetToken(requestId);

        // Отправим сообщение в брокер и ждем ответа с JWT
        return Mono.fromFuture(() -> kafkaTemplate.send("user-registration-topic", registrationMessage))
                .flatMap(result -> waitForResponse(requestId)) // Ждём ответа с JWT
                .timeout(Duration.ofSeconds(15), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Регистрация пользователя завершена"));
    }


    @PostMapping("/login")
    public Mono<Object> loginUser(@RequestBody LoginDTO loginDTO){
        loginDTO.setPassword(EncryptionUtil.encrypt(loginDTO.getPassword()));

        String requestId = UUID.randomUUID().toString(); // Генерируем уникальный идентификатор запроса
        loginDTO.setRequestId(requestId);

        log.info("Отправка сообщения логина в топик user-login-topic: {}", loginDTO);

        // Сериализация данных при логине пользователя
        String loginMessage;
        try{
            loginMessage = new ObjectMapper().writeValueAsString(loginDTO);
        } catch (JsonProcessingException e){
            log.error("Ошибка сериализации данных пользователя {} при логине: {}", loginDTO.getUsername(), e.getMessage());
            //return Mono.just("Внутренняя ошибка сервера");
            return Mono.just(ResponseEntity.status(500)
                    .body("Внутренняя ошибка сервера"));
        }
        // Сброс
        kafkaJwtListener.resetToken(requestId);

        return Mono.fromFuture(() -> kafkaTemplate.send("user-login-topic", loginMessage))
                .flatMap(result -> waitForResponse(requestId))
                .timeout(Duration.ofSeconds(15), Mono.just(ResponseEntity.status(408)
                        .body("Ошибка: ответ от Kafka не получен вовремя")))
                .doOnSuccess(aVoid -> log.info("Логин пользователя завершён"))
                .map(ResponseEntity::ok);
    }

    private Mono<Object> waitForResponse(String requestId) {
        return Mono.defer(() -> {
            String token = kafkaJwtListener.getJwtToken(requestId);
            if (token != null) {
                kafkaJwtListener.resetToken(requestId);
                return Mono.just(token);
            }
            return Mono.delay(Duration.ofMillis(500))
                    .flatMap(ignored -> waitForResponse(requestId));
        }).subscribeOn(Schedulers.parallel()); // Выполнение в параллельном потоке
    }

}