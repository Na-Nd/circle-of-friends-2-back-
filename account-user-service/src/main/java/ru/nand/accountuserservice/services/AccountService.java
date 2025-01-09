package ru.nand.accountuserservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.accountuserservice.utils.JwtUtil;
import ru.nand.sharedthings.DTO.AccountPatchDTO;


import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AccountService {
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    public AccountService(RestTemplate restTemplate, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public int getFollowersCount(String username) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + username + "/followers/count";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на получение количества подписчиков аккаунта {}", username);
        try{
            ResponseEntity<Integer> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Integer.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при получении подписчиков аккаунта {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка получения количества подписчиков: " + e.getMessage());
        }
    }

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

    public void patchAccount(AccountPatchRequest accountPatchRequest, String firstUsername) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/edit";

        // Формируем DTO для отправки на основе reauest'а
        AccountPatchDTO accountPatchDTO = new AccountPatchDTO();
        accountPatchDTO.setUsername(accountPatchRequest.getUsername());
        if(accountPatchRequest.getPassword() != null){
            accountPatchDTO.setPassword(passwordEncoder.encode(accountPatchRequest.getPassword()));
        } else {
            accountPatchDTO.setPassword(null); // Чтобы encode() не зашифровал null
        }
        accountPatchDTO.setEmail(accountPatchRequest.getEmail());
        accountPatchDTO.setFirstUsername(firstUsername);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<AccountPatchDTO> requestEntity = new HttpEntity<>(accountPatchDTO, headers);

        log.debug("Запрос к registry-service на обновление данных аккаунта пользователя");
        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка обновления данных аккаунта: {}", e.getMessage());
            throw new RuntimeException("Ошибка обновления данных аккаунта: " + e.getMessage());
        }
    }

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

    public void followUser(String currentUsername, String targetUsername) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + currentUsername + "/follow/" + targetUsername;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service на подписку текущего пользователя {} на аккаунт целевого пользователя {}", currentUsername, targetUsername);
        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка подписки пользователя {} на {}: {}", currentUsername, targetUsername, e.getMessage());
            throw new RuntimeException("Ошибка подписки на пользователя: " + e.getMessage());
        }
    }

}
