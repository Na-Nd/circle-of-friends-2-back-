package ru.nand.authservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.nand.authservice.services.KafkaJwtListener;
import ru.nand.sharedthings.DTO.LoginDTO;
import ru.nand.sharedthings.DTO.RegisterDTO;
import ru.nand.sharedthings.utils.EncryptionUtil;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaJwtListener kafkaJwtListener;

    // При регистрации будем создавать асинхронный процесс
    @PostMapping("/register")
    public Mono<Object> registerUser(@RequestBody RegisterDTO userDTO) {
        // Шифруем пароль перед отправкой
        userDTO.setPassword(EncryptionUtil.encrypt(userDTO.getPassword()));
        String expectedRequestId = UUID.randomUUID().toString();
        userDTO.setRequestId(expectedRequestId);
        log.info("Отправка сообщения регистрации в топик user-registration-topic: {}", userDTO);

        return Mono.fromFuture(() -> kafkaTemplate.send("user-registration-topic", userDTO))
                .flatMap(result -> waitForResponse(expectedRequestId)) // Ждем ответ по этому requestId
                .timeout(Duration.ofSeconds(10), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Регистрация пользователя завершена"));
    }

    // Аналогично, как и при регистрации будем создавать асинхронный процесс
    @PostMapping("/login")
    public Mono<Object> loginUser(@RequestBody LoginDTO loginDTO) {
        // Шифруем пароль перед отправкой
        loginDTO.setPassword(EncryptionUtil.encrypt(loginDTO.getPassword()));

        String requestId = UUID.randomUUID().toString(); // Генерируем уникальный идентификатор запроса
        loginDTO.setRequestId(requestId);

        log.info("Отправка сообщения логина в топик user-login-topic: {}", loginDTO);

        // Сброс состояния токена перед новым запросом
        kafkaJwtListener.resetToken(requestId);
        // Отправка сообщения в Kafka и ожидание ответа
        return Mono.fromFuture(() -> kafkaTemplate.send("user-login-topic", loginDTO))
                .flatMap(result -> waitForResponse(requestId)) // Передаём только requestId
                .timeout(Duration.ofSeconds(15), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Логин пользователя завершён"));
    }


    private Mono<Object> waitForResponse(String requestId) {
        return Mono.defer(() -> {
            String token = kafkaJwtListener.getJwtToken(requestId);
            System.out.println("Токен: " + token);
            if (token != null) {
                kafkaJwtListener.resetToken(requestId);
                return Mono.just(token);
            }
            return Mono.delay(Duration.ofSeconds(1))
                    .flatMap(ignored -> waitForResponse(requestId));
        });
    }
}