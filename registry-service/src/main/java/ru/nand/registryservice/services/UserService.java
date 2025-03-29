package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.DTO.UserDTO;
import ru.nand.registryservice.entities.ENUMS.ROLE;
import ru.nand.registryservice.entities.ENUMS.STATUS;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.entities.DTO.AccountUserService.AccountPatchDTO;
import ru.nand.registryservice.entities.DTO.AuthService.RegisterDTO;
import ru.nand.registryservice.entities.UserSession;
import ru.nand.registryservice.repositories.UserRepository;
import ru.nand.registryservice.repositories.UserSessionRepository;
import ru.nand.registryservice.utils.JwtUtil;
import ru.nand.registryservice.utils.RegistryUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper;
    private final RegistryUtil registryUtil;
    private final UserSessionRepository userSessionRepository;

    @Value("${jwt.access.jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.jwt.expiration}")
    private long refreshTokenExpiration;

    public UserSession registerUser(RegisterDTO registerDTO) {
        // Проверяем, существует ли пользователь с таким email или username
        if (userRepository.findByEmail(registerDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        if (userRepository.findByUsername(registerDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Пользователь с таким username уже существует");
        }

        // Создаем нового пользователя
        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .registrationDate(LocalDateTime.now())
                .role(ROLE.ROLE_USER)
                .build();

        // Сохраняем пользователя
        userRepository.save(user);
        log.debug("Зарегистрирован и сохранен пользователь: {}", registerDTO.getUsername());

        // Создаем сессию для нового пользователя
        return userSessionService.createSession(user);
    }

    public UserSession authenticateUser(String username, String password) {
        // Поиск пользователя по имени
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверка пароля
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        // Если пользователь найден и пароль совпал
        log.info("Пользователь {} успешно аутентифицирован", username);

        // Создаем новую сессию для пользователя
        return userSessionService.createSession(user);
    }

    public void logoutUser(String accessToken){
        try{
            userSessionService.deactivateSessionByAccessToken(accessToken);
            log.debug("Пользователь успешно вышел из системы");
        } catch (Exception e){
            log.error("Ошибка при выходе из системы: {}", e.getMessage());
            throw new RuntimeException("Ошибка при выходе из системы: " + e.getMessage());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с именем " + username + " не найден"));
    }

    public int getFollowersCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь с именем " + username + " не найден"));
        return user.getSubscribers().size();
    }

    public UserDTO getUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .subscribersCount(user.getSubscribers().size())
                .subscriptionsCount(user.getSubscriptions().size())
                .postsCount(user.getPosts().size())
                .build();
    }

    public List<String> getFollowers(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь с именем " + username + " не найден"));
        return user.getSubscribers().stream()
                .map(User::getUsername)
                .toList();
    }

    public List<String> getFollowing(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь с именем " + username + " не найден"));
        return user.getSubscriptions().stream()
                .map(User::getUsername)
                .toList();
    }

    public String followUser(String currentUsername, String targetUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь " + currentUsername + " не найден"));
        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь " + targetUsername + " не найден"));

        if (currentUser.getSubscriptions().contains(targetUser)) {
            throw new RuntimeException("Пользователь уже подписан");
        }

        currentUser.getSubscriptions().add(targetUser);
        targetUser.getSubscribers().add(currentUser);

        userRepository.save(currentUser);
        userRepository.save(targetUser);

        log.debug("Пользователь {} подписался на {}", currentUsername, targetUsername);

        return targetUser.getEmail();
    }

    @Transactional
    public void unfollowUser(String currentUsername, String targetUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь " + currentUsername + " не найден"));
        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь " + targetUsername + " не найден"));

        if (!currentUser.getSubscriptions().contains(targetUser)) {
            throw new RuntimeException("Пользователь не подписан");
        }

        currentUser.getSubscriptions().remove(targetUser);
        targetUser.getSubscribers().remove(currentUser);

        userRepository.save(currentUser);
        userRepository.save(targetUser);

        log.debug("Пользователь {} отписался от {}", currentUsername, targetUsername);
    }

    public List<String> getAllUsernames() {
        return userRepository.findAll().stream()
                .map(User::getUsername)
                .toList();
    }

    @Transactional
    public String updateUser(AccountPatchDTO accountPatchDTO) {
        User user = userRepository.findByUsername(accountPatchDTO.getFirstUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Обновить username, если он был изменен и не занят
        if (accountPatchDTO.getUsername() != null && !accountPatchDTO.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(accountPatchDTO.getUsername()).isPresent()) {
                throw new RuntimeException("Username уже используется");
            }
            user.setUsername(accountPatchDTO.getUsername());
        }

        // Обновить email, если он был изменен и не занят
        if (accountPatchDTO.getEmail() != null && !accountPatchDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(accountPatchDTO.getEmail()).isPresent()) {
                throw new RuntimeException("Email уже используется");
            }
            user.setEmail(accountPatchDTO.getEmail());
        }

        if (accountPatchDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(accountPatchDTO.getPassword()));
        }

        userRepository.save(user);
        log.debug("Данные пользователя обновлены: {}", user);

        // Находим активную сессию пользователя
        List<UserSession> activeSessions = userSessionService.findByUserAndStatus(user, STATUS.ACTIVE);
        if (activeSessions.isEmpty()) {
            throw new RuntimeException("Активная сессия не найдена");
        }

        UserSession session = activeSessions.getFirst();

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        // Обновляем сессию с новыми токенами
        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setAccessTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpiration)));
        session.setRefreshTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)));
        session.setLastActivityTime(LocalDateTime.now());

        userSessionService.save(session);

        log.debug("Сессия пользователя обновлена с новыми токенами: {}", user.getUsername());

        return newAccessToken;
    }

    public void deleteUser(String username) {
        userRepository.deleteByUsername(username);
    }

    public String existsById(int userId) {
        return userRepository.findById(userId).
                orElseThrow(()-> new RuntimeException("Пользователь не найден"))
                .getEmail();
    }

    /// Возвращает JSON ответ из почт пользователей если все пользователи существуют
    public String findUsers(String requestMessage){
        Set<Integer> usersIds;
        try{
            usersIds = objectMapper.readValue(requestMessage, new TypeReference<Set<Integer>>(){});
        } catch (Exception e){
            log.warn("Ошибка при десериализации запроса {}: {}", requestMessage, e.getMessage());
            throw new RuntimeException("Ошибка при десериализации данных запроса " + e.getMessage());
        }

        Set<String> usersEmails = new HashSet<>();

        try{
            for(Integer userId : usersIds){
                usersEmails.add(
                        userRepository.findById(userId).orElseThrow(()-> new RuntimeException("Пользователь не найден")).getEmail()
                );
            }
        } catch (RuntimeException e){ // Если хотя бы один из пользователей не найден - бросаем исключение в контроллер
            log.warn("Ошибка при поиске пользователей: {}", e.getMessage());
            throw new RuntimeException("Ошибка при поиске пользователей: " + e.getMessage());
        }

        // Собираем в JSON
        String responseMessage;
        try{
            responseMessage = objectMapper.writeValueAsString(usersEmails);
        } catch (Exception e){
            log.warn("Ошибка при сериализации данных овтета: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сериализации данных ответа: " + e.getMessage());
        }

        return responseMessage;
    }

    /// Получение списка созданных аккаунтов за последние n часов
    public String getCreatedAccounts(int hoursCount) {
        try{
            // Текущее время и время, которое было n часов назад
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusHours(hoursCount);

            // Получаем список пользователей, зарегистрированных за последние n часов
            List<User> users = userRepository.findByRegistrationDateBetween(startTime, now);

            return registryUtil.enrichUserDTOs(users);
        } catch (Exception e){
            log.warn("Ошибка при получении списка созданных аккаунтов за последние {} часов: {}", hoursCount, e.getMessage());
            throw new RuntimeException("Ошибка при получении списка созданных аккаунтов за последние " + hoursCount + "часов: " + e.getMessage());
        }
    }

    /// Получение N аккаунтов с наибольшим количеством подписчиков (количество упорядочено от большего к меньшему)
    /// если пользователей меньше N то вернет всех доступных
    public String getPopularAccounts(int accountsCount) {
        try {
            List<User> users = userRepository.findAll();

            // Сортируем пользователей по количеству подписчиков в порядке убывания
            List<User> sortedUsers = users.stream()
                    .sorted((u1, u2) -> Integer.compare(u2.getSubscribers().size(), u1.getSubscribers().size()))
                    .toList();

            // Берем первые accountsCount пользователей
            List<User> topUsers = sortedUsers.stream()
                    .limit(accountsCount)
                    .toList();

            // Преобразуем список User в список UserDTO
            return registryUtil.enrichUserDTOs(topUsers);
        } catch (Exception e) {
            log.warn("Ошибка при получении популярных аккаунтов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении популярных аккаунтов: " + e.getMessage());
        }
    }

    /// Получение списка аккаунтов с заблокированными сессиями (типа подозрительная активность)
    public String getBlockedAccounts() {
        try{
            // Все заблокированные сессии
            List<UserSession> blockedSessions = userSessionRepository.findByStatus(STATUS.BLOCKED);

            // Берем пользователей
            List<User> blockedUsers = blockedSessions.stream()
                    .map(UserSession::getUser)
                    .distinct()
                    .toList();

            // Преобразуем в ДТО
            List<UserDTO> response = blockedUsers.stream()
                    .map(user -> UserDTO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .subscribersCount(user.getSubscribers().size())
                            .subscriptionsCount(user.getSubscriptions().size())
                            .postsCount(user.getPosts().size())
                            .build()).toList();

            // Соберем в JSON
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e){
            log.warn("Ошибка при сериализации данных: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сериализации данных");
        }
    }
}