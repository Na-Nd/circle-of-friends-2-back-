package ru.nand.authservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.nand.authservice.entities.DTO.LoginDTO;
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

        // Сохраняем данные пользователя
        pendingRegistrations.put(userDTO.getEmail(), userDTO);

        mailSenderService.sendMail(
                userDTO.getEmail(),
                "Код верификации",
                "Здравствуйте, " + userDTO.getUsername() + ", ваш код верификации: " + verificationCode
        );

        log.info("Код подтверждения отправлен на email: {}", userDTO.getEmail());
        return Mono.just("Код верификации отправлен. Проверьте свою почту.");
    }

    @PostMapping("/verify-email")
    public Mono<Object> verifyAndRegisterUser(@RequestParam String email, @RequestParam String code) throws JsonProcessingException {
        String savedCode = emailVerificationCodes.get(email);
        Long timestamp = verificationTimestamps.get(email);
        RegisterDTO userDTO = pendingRegistrations.get(email);

        if (savedCode == null || timestamp == null || userDTO == null) {
            return Mono.just("Код верификации истек или неверен. Попробуйте зарегистрироватся снова.");
        }

        if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) { // 5 минут
            emailVerificationCodes.remove(email);
            verificationTimestamps.remove(email);
            pendingRegistrations.remove(email);
            return Mono.just("Код верификации истек. Попробуйте зарегистриороваться снова.");
        }

        if (!savedCode.equals(code)) {
            return Mono.just("Неверный код верификации.");
        }

        emailVerificationCodes.remove(email);
        verificationTimestamps.remove(email);

        log.info("Email {} успешно подтвержден", email);

        userDTO.setEmailVerified(true);
        userDTO.setPassword(EncryptionUtil.encrypt(userDTO.getPassword())); // Шифруем пароль перед отправкой в кафку
        String requestId = UUID.randomUUID().toString();
        userDTO.setRequestId(requestId);

        log.debug("Отправка сообщения регистрации в топик user-registration-topic: {}", userDTO);

        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(userDTO);

        // Сброс токена для нового запроса
        kafkaJwtListener.resetToken(requestId);

        // Отправим сообщение в брокер и ждем ответа с JWT
        return Mono.fromFuture(() -> kafkaTemplate.send("user-registration-topic", message))
                .flatMap(result -> waitForResponse(requestId)) // Ждём ответа с JWT
                .timeout(Duration.ofSeconds(15), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Регистрация пользователя завершена"));
    }


    @PostMapping("/login")
    public Mono<Object> loginUser(@RequestBody LoginDTO loginDTO) throws JsonProcessingException {
        loginDTO.setPassword(EncryptionUtil.encrypt(loginDTO.getPassword()));

        String requestId = UUID.randomUUID().toString(); // Генерируем уникальный идентификатор запроса
        loginDTO.setRequestId(requestId);

        log.info("Отправка сообщения логина в топик user-login-topic: {}", loginDTO);

        ObjectMapper objectMapper = new ObjectMapper();

        // TODO мб try-catch
        String message = objectMapper.writeValueAsString(loginDTO);
        // Сброс
        kafkaJwtListener.resetToken(requestId);

        return Mono.fromFuture(() -> kafkaTemplate.send("user-login-topic", message))
                .flatMap(result -> waitForResponse(requestId)) // Передаём только requestId
                .timeout(Duration.ofSeconds(15), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Логин пользователя завершён"));
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