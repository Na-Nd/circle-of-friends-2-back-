package ru.nand.accountuserservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.sharedthings.DTO.AccountPatchDTO;
import ru.nand.sharedthings.utils.KeyGenerator;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AccountService {
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${interservice.secret.key}")
    private String SECRET_KEY;

    @Value("${myplug}") // Заглушка, так как метод для шифрования написан для шифрования токена
    private String TOKEN_VALUE;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Autowired
    public AccountService(RestTemplate restTemplate, PasswordEncoder passwordEncoder) {
        this.restTemplate = restTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    private HttpHeaders buildHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, KeyGenerator.generateKey(SECRET_KEY, TOKEN_VALUE));
        return headers;
    }

    public int getFollowersCount(String username){
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/followers/count";
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на получение количества подписчиков пользователя {}", username);

        try{
            ResponseEntity<Integer> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Integer.class
            );
            // TODO: Если в response null, то положить "нет подписчиков"
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка получения количества подписчиков для пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения количества подписчиков");
        }
    }

    public List<String> getAllUsernames(){
        String url = REGISTRY_SERVICE_URL + "/api/users";
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на получение списка username'ов всех аккаунтов");

        try{
            ResponseEntity<String[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String[].class
            );
            // TODO: Если в response null, то положить "нет пользователей"

            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Ошибка при получении списка пользователей: {}", e.getMessage());
            throw new RuntimeException("Ошибка получения списка пользователей");
        }
    }

    public void patchAccount(AccountPatchRequest accountPatchRequest, String firstUsername){
        String url = REGISTRY_SERVICE_URL + "/api/users/edit";

        // Формируем дто на основе request'а
        AccountPatchDTO accountPatchDTO = new AccountPatchDTO();
        accountPatchDTO.setUsername(accountPatchRequest.getUsername());
        // Отправим сразу зашифрованный пароль если он не null
        if(accountPatchRequest.getPassword() != null){
            accountPatchDTO.setPassword(passwordEncoder.encode(accountPatchRequest.getPassword()));
        } else {
            accountPatchDTO.setPassword(null); // Чтобы encode() не зашифровал null, а то непонятно что будет
        }
        accountPatchDTO.setEmail(accountPatchRequest.getEmail());
        accountPatchDTO.setFirstUsername(firstUsername);

        HttpEntity<AccountPatchDTO> request = new HttpEntity<>(accountPatchDTO, buildHeaders());
        log.debug("Запрос к registry-service на обновление данных аккаунта пользователя");

        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    Void.class
            );
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Ошибка обновления данных аккаунта");
        }
    }

    public void deleteAccount(String username){
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username;
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на удаление аккаунта пользователя {}", username);

        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка удаления аккаунта пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка удаления аккаунта");
        }
    }

    public List<String> getFollowers(String username){
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/followers";
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на получение списка подписчиков пользователя {}", username);

        try {
            ResponseEntity<String[]> response = restTemplate.exchange(url, HttpMethod.GET, request, String[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Ошибка получения подписчиков пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения подписчиков");
        }
    }

    public List<String> getFollowing(String username) {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/following";
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на получение списка подписок пользователя {}", username);

        try {
            ResponseEntity<String[]> response = restTemplate.exchange(url, HttpMethod.GET, request, String[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Ошибка получения подписок пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения подписок");
        }
    }

    public void followUser(String currentUsername, String targetUsername) {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + currentUsername + "/follow/" + targetUsername;
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        log.debug("Запрос к registry-service на подписку текущего пользователя {} на аккаунт целевого пользователя {}", currentUsername, targetUsername);

        try {
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
        } catch (Exception e) {
            log.error("Ошибка подписки пользователя {} на {}: {}", currentUsername, targetUsername, e.getMessage());
            throw new RuntimeException("Ошибка подписки на пользователя");
        }
    }

}
