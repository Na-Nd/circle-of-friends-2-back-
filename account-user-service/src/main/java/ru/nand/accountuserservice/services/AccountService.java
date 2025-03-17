package ru.nand.accountuserservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.accountuserservice.entities.DTO.UserDTO;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.accountuserservice.utils.AccountUtil;
import ru.nand.accountuserservice.utils.JwtUtil;
import ru.nand.accountuserservice.entities.DTO.AccountPatchDTO;
import ru.nand.accountuserservice.entities.DTO.NotificationDTO;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AccountUtil accountUtil;
    private final RedisService redisService;
    private final MailSenderService mailSenderService;
    private final ObjectMapper objectMapper;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Обновление аккаунта
    @CacheEvict(value = "userCache", key = "#username")
    public String editAccount(String username, AccountPatchRequest accountPatchRequest) {
        try {
            // Если в запросе новая почта, то отправим код верификации почты
            if (accountPatchRequest.getEmail() != null) {
                log.info("В запросе на изменение аккаунта указана новая почта, отправим код верификации");
                String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);
                redisService.saveWithExpiration("verification_code:" + username, verificationCode, 5, TimeUnit.MINUTES);
                redisService.saveWithExpiration("pending_edit:" + username, accountPatchRequest, 5, TimeUnit.MINUTES);

                mailSenderService.sendMail(
                        accountPatchRequest.getEmail(),
                        "Подтверждение изменения почты",
                        "Здравствуйте, " + username + ", ваш код для подтверждения почты: " + verificationCode
                );

                log.info("Отправлен код верификации на новую почту: {}", accountPatchRequest.getEmail());
                return "Код для подтверждения новой почты отправлен на: " + accountPatchRequest.getEmail() + ", проверьте почту";
            }

            // Если пользователь не обновлял почту
            String newJwt = patchAccount(accountPatchRequest, username);
            log.info("Запрос на обновление данных аккаунта пользователя {}", username);
            return "Аккаунт успешно обновлен, accessToken: " + newJwt;
        } catch (Exception e) {
            log.warn("Ошибка при обновлении аккаунта: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обновлении аккаунта пользователя  " + username + ": " + e.getMessage());
        }
    }

    /// Верификация
    public String verifyEmail(String username, String code, String authorizationHeader) {
        String savedCode = (String) redisService.get("verification_code:" + username);
        AccountPatchRequest pendingRequest = redisService.getAccountPatchRequest("pending_edit:" + username);

        if (savedCode == null || pendingRequest == null) {
            throw new RuntimeException("Код подтверждения истек или неверен");
        }

        if (!savedCode.equals(code)) {
            throw new RuntimeException("Неверный код подтверждения");
        }

        // Чистим Redis после успешной верификации
        redisService.delete("verification_code:" + username);
        redisService.delete("pending_edit:" + username);

        // Таким костылем достаем старую почту из токена текущего пользователя
        String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
        String email = jwtUtil.extractEmail(token);

        // Создаем уведомление и отправляем на старую почту
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .userEmail(email)
                .message(username + ", ваша почта была изменена на: " + pendingRequest.getEmail())
                .build();

        String oldEmailNotificationMessage;
        try {
            oldEmailNotificationMessage = objectMapper.writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка сериализации уведомления для старой почты: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации уведомления для старой почты: " + e.getMessage());
        }

        // Перезаписываем уведомление и отправляем на новую почту
        notificationDTO.setUserEmail(pendingRequest.getEmail());
        notificationDTO.setMessage(username + ", данные вашего аккаунта были изменены");
        String newEmailNotificationMessage;
        try {
            newEmailNotificationMessage = objectMapper.writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка сериализации уведомления для новой почты: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации уведомления для новой почты: " + e.getMessage());
        }

        try {
            String newToken = patchAccount(pendingRequest, username);
            log.info("Почта пользователя {} была успешно подтверждена и данные аккаунта обновлены", username);

            // Отправляем уведомления
            accountUtil.sendNotification(email, oldEmailNotificationMessage);
            accountUtil.sendNotification(pendingRequest.getEmail(), newEmailNotificationMessage);

            return username + ", данные вашего аккаунта успешно обновлены, accessToken: " + newToken;
        } catch (Exception e) {
            log.error("Ошибка обновления аккаунта пользователя {} после подтверждения почты: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка при обновлении данных аккаунта: "+ e.getMessage());
        }
    }

    /// Обновление аккаунта
    private String patchAccount(AccountPatchRequest accountPatchRequest, String firstUsername) {
        String url = REGISTRY_SERVICE_URL + "/api/users/edit";

        // Формируем DTO для отправки
        AccountPatchDTO accountPatchDTO = new AccountPatchDTO();
        accountPatchDTO.setUsername(accountPatchRequest.getUsername());
        accountPatchDTO.setPassword(
                accountPatchRequest.getPassword() != null ? passwordEncoder.encode(accountPatchRequest.getPassword()) : null
        );
        accountPatchDTO.setEmail(accountPatchRequest.getEmail());
        accountPatchDTO.setFirstUsername(firstUsername);

        String requestMessage;
        try {
            requestMessage = objectMapper.writeValueAsString(accountPatchDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации данных запроса для обновления аккаунта", e);
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);

        log.debug("Запрос к registry-service на обновление данных аккаунта пользователя");
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешной ответ от registry-service при обновлении аккаунта пользователя {}: {}", firstUsername, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service при обновлении аккаунта пользователя " + firstUsername + ": " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при обновлении данных аккаунта: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка обновления данных аккаунта: " + e.getMessage(), e);
        }
    }

    /// Получение данных аккаунта конкретного пользователя
    @Cacheable(value = "userCache", key = "#username", unless = "#result == null")
    public UserDTO getUserByUsername(String username){
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на получение данных аккаунта пользователя {}", username);
        try {
            ResponseEntity<UserDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    UserDTO.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении данных об аккаунте пользователя {}: {}", username, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service при получении данных об аккаунте пользователя " + username + ": " + response.getStatusCode());
            }

            System.out.println(response.getBody());
            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при получении данных аккаунта пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка при получении данных аккаунта пользователя: " + username + ": " + e.getMessage());
        }
    }

    /// Получение списка username'ов
    public List<String> getAllUsernames() throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на получение списка username'ов всех аккаунтов");
        try{
            ResponseEntity<String []> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String[].class
            );

            return Arrays.asList(response.getBody());
        } catch (Exception e){
            log.error("Ошибка при получении списка пользователей: {}", e.getMessage());
            throw new RuntimeException("Ошибка получения списка пользователей: " + e.getMessage());
        }
    }

    /// Удаление аккаунта
    public void deleteAccount(String username) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на удаление аккаунта пользователя {}", username);
        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка удаления аккаунта пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка удаления аккаунта: " + e.getMessage());
        }
    }

    /// Получение списка username'ов подписчиков
    public List<String> getFollowers(String username) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/followers";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на получение списка подписчиков пользователя {}", username);
        try{
            ResponseEntity<String[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String[].class
            );

            return Arrays.asList(response.getBody());
        } catch (Exception e){
            log.error("Ошибка получения подписчиков пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения подписчиков: " + e.getMessage());
        }
    }

    /// Получение username'ов подписок
    public List<String> getFollowing(String username) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/following";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на получение списка подписок пользователя {}", username);
        try{
            ResponseEntity<String[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String[].class
            );

            return Arrays.asList(response.getBody());
        } catch (Exception e){
            log.error("Ошибка получения подписок пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения подписок: " + e.getMessage());
        }
    }

    /// Подписаться на пользователя
    public void followUser(String currentUsername, String targetUsername) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + currentUsername + "/follow/" + targetUsername;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на подписку текущего пользователя {} на аккаунт целевого пользователя {}", currentUsername, targetUsername);
        try {
            // Возвращается email пользователя с targetUsername
            ResponseEntity<String> response =restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при подписке пользователя: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service при подписке на пользователя: " + targetUsername);
            }

            // Отправляем уведомление пользователю о подписке на его аккаунт
            accountUtil.sendNotification(response.getBody(), "На вас подписался " + currentUsername);

        } catch (Exception e){
            log.error("Ошибка подписки пользователя {} на {}: {}", currentUsername, targetUsername, e.getMessage());
            throw new RuntimeException("Ошибка подписки на пользователя: " + e.getMessage());
        }
    }

    /// Отписаться от пользователя
    public void unfollowUser(String currentUsername, String targetUsername){
        String url = REGISTRY_SERVICE_URL + "/api/users/" + currentUsername + "/unfollow/" + targetUsername;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на отписку пользователя {} от {}", currentUsername, targetUsername);

        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка при отписке пользователя {} от {}: {}", currentUsername, targetUsername, e.getMessage());
            throw new RuntimeException("Ошибка отписки от пользователя: " + e.getMessage());
        }
    }
}
