package ru.nand.accountuserservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import ru.nand.accountuserservice.entities.DTO.NotificationDTO;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.accountuserservice.services.AccountService;
import ru.nand.accountuserservice.services.MailSenderService;
import ru.nand.accountuserservice.utils.JwtUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final MailSenderService mailSenderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JwtUtil jwtUtil;

    private final Map<String, String> emailVerificationCodes = new HashMap<>();
    private final Map<String, Long> verificationTimestamps = new HashMap<>();
    private final Map<String, AccountPatchRequest> pendingEdits = new HashMap<>();

    @Autowired
    public AccountController(AccountService accountService, MailSenderService mailSenderService, KafkaTemplate<String, String> kafkaTemplate, JwtUtil jwtUtil) {
        this.accountService = accountService;
        this.mailSenderService = mailSenderService;
        this.kafkaTemplate = kafkaTemplate;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public String test(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            String username = userDetails.getUsername();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("NO_ROLE");

            log.debug("Пользователь: {}, Роль: {}", username, role);

            return String.format("Hello, %s! Your role is %s.", username, role);
        } else {
            log.error("Пользователь не найден");
            return "Пользователь не найден";
        }
    }

    // Получить кол-во подписчиков конкретного пользователя
    @GetMapping("/user/{username}")
    public ResponseEntity<String> getUserByUsername(@PathVariable String username) {
        try{
            int followersCont = accountService.getFollowersCount(username);
            return ResponseEntity.status(200).body("Пользователь: " + username + ", количество подписчиков: " + followersCont);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }

    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(){
        try{
            List<String> usernames = accountService.getAllUsernames();
            log.info("Был получен список пользователей");
            return ResponseEntity.status(200).body(usernames);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: "+ e.getMessage());
        }

    }
    
    @PatchMapping("/edit")
    public ResponseEntity<String> editAccount(
            @AuthenticationPrincipal UserDetails userDetails, @RequestBody AccountPatchRequest accountPatchRequest) {
        try{
            String username = userDetails.getUsername();

            // Если в запросе новая почта, то отправим код верификации почты
            if(accountPatchRequest.getEmail() != null){
                log.info("В запросе на изменение аккаунта указана новая почта, отправим код верификации");
                String verificationCode = String.valueOf((int) (Math.random() * 9000) + 1000);
                emailVerificationCodes.put(username, verificationCode);
                verificationTimestamps.put(username, System.currentTimeMillis());
                pendingEdits.put(username, accountPatchRequest);

                mailSenderService.sendMail(
                        accountPatchRequest.getEmail(),
                        "Подтверждение изменения почты",
                        "Здравствуйте, " + username + ", ваш код для подтверждения почты: " + verificationCode
                );

                log.info("Отправлен код верификации на новую почту: {}", accountPatchRequest.getEmail());
                return ResponseEntity.status(200).body("Код для подтверждения новой почты отправлен на: " + accountPatchRequest.getEmail() + ", проверьте почту");
            }

            // Если пользователь не обновлял почту
            accountService.patchAccount(accountPatchRequest, username);
            log.info("Запрос на обновление данных аккаунта пользователя {}", username);
            return ResponseEntity.status(200).body("Аккаунт успешно обновлен");
        } catch (Exception e){
            log.error("Ошибка при обновлении аккаунта: {}", e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при обновлении аккаунта: " + e.getMessage());
        }
    }

    // TODO service & util
    // Сюда редирект с editAccount(), попадаем только если в реквесте пользователь хочет изменить почту
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String code,
            @RequestHeader("Authorization") String authorizationHeader){
        String username = userDetails.getUsername();;
        String savedCode = emailVerificationCodes.get(username);
        Long timestamp = verificationTimestamps.get(username);
        AccountPatchRequest pendingRequest = pendingEdits.get(username);

        if (savedCode == null || timestamp == null || pendingRequest == null) {
            return ResponseEntity.status(400).body("Код подтверждения истек или неверен");
        }

        // Срок жизни кода верификации
        if(System.currentTimeMillis() - timestamp > 5 * 60 * 1000){
            emailVerificationCodes.remove(username);
            verificationTimestamps.remove(username);
            pendingEdits.remove(username);

            return ResponseEntity.status(400).body("Код подтверждения истек");
        }

        // Правильность кода
        if(!savedCode.equals(code)){
            return ResponseEntity.status(400).body("Неверный код подтверждения");
        }

        // Чистим мапы после успешной верификации
        emailVerificationCodes.remove(username);
        verificationTimestamps.remove(username);

        // Таким костылем достаем старую почту из токена текущего пользователя
        String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
        String email = jwtUtil.extractEmail(token);

        // Создаем уведомление и отправляем на старую почту
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setUserEmail(email);
        notificationDTO.setMessage(username + ", ваша почта была изменена на: " + pendingRequest.getEmail());
        String oldEmailNotificationMessage;
        try{
            oldEmailNotificationMessage = new ObjectMapper().writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации уведомления для старой почты: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
        //kafkaTemplate.send("user-notifications-topic", oldEmailNotificationMessage);

        // Перезаписываем уведомление и отправляем на новую почту
        notificationDTO.setUserEmail(pendingRequest.getEmail());
        notificationDTO.setMessage(username + ", данные вашего аккаунта были изменены");
        String newEmailNotificationMessage;
        try{
            newEmailNotificationMessage = new ObjectMapper().writeValueAsString(notificationDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации уведомления для новой почты: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
        // kafkaTemplate.send("user-notifications-topic", newEmailNotificationMessage);

        try{
            accountService.patchAccount(pendingRequest, username);
            pendingEdits.remove(username);
            log.info("Почта пользователя {} была успешно подтверждена и данные аккаунта обновлены", username);

            // Отправляем уведомления
            kafkaTemplate.send("user-notifications-topic", oldEmailNotificationMessage);
            kafkaTemplate.send("user-notifications-topic", newEmailNotificationMessage);

            // Генерируем токен на основе новых данных
            String newToken;
            // Если пользователь сменил username
            if(pendingRequest.getUsername() != null){
                newToken = jwtUtil.generateToken(pendingRequest.getUsername(), jwtUtil.extractRole(token), pendingRequest.getEmail());
            } else{
                // В противном случае создаем токен на основе старого username, который достаем из токена текущего пользователя
                newToken = jwtUtil.generateToken(jwtUtil.extractUsername(token), jwtUtil.extractRole(token), pendingRequest.getEmail());
            }

            return ResponseEntity.status(200).body(username + ", данные вашего аккаунта успешно обновлены, JWT: " + newToken);
        } catch (Exception e){
            log.error("Ошибка обновления аккаунта пользователя {} после подтверждения почты: {}", username, e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при обновлении данных аккаунта");
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        try{
            accountService.deleteAccount(userDetails.getUsername());
            log.info("Удаление аккаунта пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body("Аккаунт пользователя " + userDetails.getUsername() + " удален");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления аккаунта");
        }
    }

    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers(@AuthenticationPrincipal UserDetails userDetails){
        try {
            List<String> followers = accountService.getFollowers(userDetails.getUsername());
            log.info("Получен список подписчиков пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(followers);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка получения списка подписчиков пользователя");
        }
    }

    @GetMapping("/following")
    public ResponseEntity<?> getFollowing(@AuthenticationPrincipal UserDetails userDetails){
        try{
            List<String> following = accountService.getFollowing(userDetails.getUsername());
            log.info("Получен список подписок пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(following);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка получения списка подписок пользователя");
        }
    }

    @PostMapping("/follow/{targetUsername}")
    public ResponseEntity<String> followUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String targetUsername) {
        try{
            accountService.followUser(userDetails.getUsername(), targetUsername);
            log.info("Запрос пользователя {} подписаться на {}", userDetails.getUsername(), targetUsername);
            return ResponseEntity.status(200).body("Теперь вы подписаны на пользователя " + targetUsername);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка подписки на пользователя");
        }
    }

    // TODO Отписка
    @DeleteMapping("/follow/{targetUsername}")
    public ResponseEntity<String> unfollowUser(@PathVariable String targetUsername, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            accountService.unfollowUser(userDetails.getUsername(), targetUsername);
            log.info("Запрос пользователя {} на отписку от {}", userDetails.getUsername(), targetUsername);

            return ResponseEntity.status(200).body("Вы отписались от пользователя " + targetUsername);
        } catch (Exception e){
            log.error("Ошибка при отписке: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка отписки от пользователя");
        }
    }

}
