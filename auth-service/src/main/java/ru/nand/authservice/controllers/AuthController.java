package ru.nand.authservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.nand.authservice.services.KafkaJwtListener;
import ru.nand.authservice.entities.DTO.LoginDTO;
import ru.nand.authservice.entities.DTO.RegisterDTO;
import ru.nand.authservice.utils.EncryptionUtil;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaJwtListener kafkaJwtListener;

    @PostMapping("/register")
    public Mono<Object> registerUser(@RequestBody RegisterDTO userDTO) throws JsonProcessingException {
        // Шифруем пароль перед отправкой
        userDTO.setPassword(EncryptionUtil.encrypt(userDTO.getPassword()));
        String expectedRequestId = UUID.randomUUID().toString();
        userDTO.setRequestId(expectedRequestId);
        log.info("Отправка сообщения регистрации в топик user-registration-topic: {}", userDTO);

        ObjectMapper objectMapper = new ObjectMapper();
        // TODO мб try-catch
        String message = objectMapper.writeValueAsString(userDTO);

        return Mono.fromFuture(() -> kafkaTemplate.send("user-registration-topic", message))
                .flatMap(result -> waitForResponse(expectedRequestId)) // Ждем ответ по этому requestId
                .timeout(Duration.ofSeconds(10), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Регистрация пользователя завершена"));
    }

    @PostMapping("/login")
    public Mono<Object> loginUser(@RequestBody LoginDTO loginDTO) throws JsonProcessingException {
        // Шифруем пароль перед отправкой
        loginDTO.setPassword(EncryptionUtil.encrypt(loginDTO.getPassword()));

        String requestId = UUID.randomUUID().toString(); // Генерируем уникальный идентификатор запроса
        loginDTO.setRequestId(requestId);

        log.info("Отправка сообщения логина в топик user-login-topic: {}", loginDTO);

        ObjectMapper objectMapper = new ObjectMapper();

        // TODO мб try-catch
        String message = objectMapper.writeValueAsString(loginDTO);
        // Сброс токена
        kafkaJwtListener.resetToken(requestId);
        // Отправка сообщения в Kafka и ожидание ответа
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