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

        log.info("Отправка сообщения регистрации в топик user-registration-topic: {}", userDTO);

        // Отправка сообщения в Kafka и ожидание ответа
        return Mono.fromFuture(() -> kafkaTemplate.send("user-registration-topic", userDTO)) // kafkaTemplate.send - отправляет сообщение в брокер и возвращает CompletableFuture, который отслеживает статус отправки. Mono.fromFuture оборачивает полученный CompletableFuture в реактивный объект Mono //TODO что это такое Mono и как работает
                .flatMap(result -> waitForResponse("user-registration-response-topic", userDTO.getUsername())) // После отправки сообщения дожидаемся ответа от брокера (waitForResponse - логика ожидания ответа) // TODO что такое flatMap и как работает?
                .timeout(Duration.ofSeconds(10), Mono.just("Ошибка: ответ от Kafka не получен вовремя")) // Если ответ не пришел, вернем ошибку // TODO что такое Mono.just?
                .doOnSuccess(aVoid -> log.info("Регистрация пользователя завершена")); // Если метод завершился успешно
    }

    // Аналогично, как и при регистрации будем создавать асинхронный процесс
    @PostMapping("/login")
    public Mono<Object> loginUser(@RequestBody LoginDTO loginDTO) {
        // Шифруем пароль перед отправкой
        loginDTO.setPassword(EncryptionUtil.encrypt(loginDTO.getPassword()));

        log.info("Отправка сообщения логина в топик user-login-topic: {}", loginDTO);

        // Сброс предыдущего состояния токена перед новым запросом
        kafkaJwtListener.resetToken();

        // Отправка сообщения в Kafka и ожидание ответа
        return Mono.fromFuture(() -> kafkaTemplate.send("user-login-topic", loginDTO))
                .flatMap(result -> waitForResponse("user-login-response-topic", loginDTO.getUsername()))
                .timeout(Duration.ofSeconds(10), Mono.just("Ошибка: ответ от Kafka не получен вовремя"))
                .doOnSuccess(aVoid -> log.info("Логин пользователя завершен"));
    }

    private Mono<Object> waitForResponse(String topic, String username) {
        return Mono.defer(() -> Mono.create(sink -> { // Mono.defer - Создает новый Mono каждый раз при вызове, чтобы внутренняя логика выполнялась заново для каждого нового вызова. Mono.create - Позволяет вручную отправлять события завершения (успех / ошибка) в реактивную цепочку
                    // Запускаем цикл ожидания токена
                    new Thread(() -> {
                        int retries = 20; // Максимальное количество попыток (20 раз по 1000мс)
                        String token = null;

                        for (int i = 0; i < retries; i++) {
                            token = kafkaJwtListener.getJwtToken();

                            if (token != null) {
                                sink.success(token); // Успешно завершаем поток с токеном
                                return;
                            }

                            try {
                                Thread.sleep(1000); // Ждём секунду перед следующей попыткой (или 500мс)
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                sink.error(new RuntimeException("Поток ожидания был прерван", e));
                                return;
                            }
                        }

                        sink.success("Ошибка: JWT не получен"); // Возвращаем ошибку, если время ожидания истекло
                    }).start();
                }))
                .doOnSuccess(token -> log.info("Получен JWT токен: {}", token))
                .doOnError(error -> log.error("Ошибка при получении токена: {}", error.getMessage()));
    }

}