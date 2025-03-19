package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.ENUMS.ROLE;
import ru.nand.registryservice.entities.ENUMS.STATUS;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.entities.UserSession;
import ru.nand.registryservice.repositories.UserRepository;
import ru.nand.registryservice.repositories.UserSessionRepository;
import ru.nand.registryservice.utils.JwtUtil;
import ru.nand.registryservice.utils.RegistryUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserSessionService {
    private final UserSessionRepository userSessionRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RegistryUtil registryUtil;

    @Value("${jwt.access.jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.jwt.expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public UserSessionService(UserSessionRepository userSessionRepository, JwtUtil jwtUtil, UserRepository userRepository, RegistryUtil registryUtil) {
        this.userSessionRepository = userSessionRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.registryUtil = registryUtil;
    }

    /// Создание новой сессия для пользователя
    public UserSession createSession(User user) {
        // Проверка на наличие заблокированных сессий
        if(hasBlockedSessions(user)){
            log.error("Пользователь {} имеет заблокированные сессии. Новая сессия не создана.", user.getUsername());
            throw new RuntimeException("Сессии пользователя заблокированы");
        }

        // Проверка на активные сессии
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);
        if (!activeSessions.isEmpty()) {
            log.warn("При создании новой сессии обнаружено несколько активных сессий для пользователя {}. Все сессии будут заблокированы.", user.getUsername());
            blockUserSessions(user);
            throw new RuntimeException("Обнаружено несколько активных сессий. Все сессии заблокированы.");
        }

        UserSession session = UserSession.builder()
                .user(user)
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .accessTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpiration)))
                .refreshTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                .sessionCreationTime(LocalDateTime.now())
                .lastActivityTime(LocalDateTime.now())
                .status(STATUS.ACTIVE)
                .build();

        log.debug("Создана сессия для пользователя: {}", user.getUsername());

        return userSessionRepository.save(session);
    }

    /// Обновление access токена
    public UserSession refreshAccessToken(String refreshToken) {
        // Проверка на наличие активной сессии
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));

        if (session.getStatus() != STATUS.ACTIVE) {
            throw new RuntimeException("Сессия не активна");
        }

        // Проверка на наличие заблокированных сессий
        if (hasBlockedSessions(session.getUser())) {
            throw new RuntimeException("Невозможно обновить access токен: есть заблокированные сессии");
        }

        // Проверка на активные сессии
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(session.getUser(), STATUS.ACTIVE);
        if (activeSessions.size() > 1) { // Если больше одной - это подозрительная активность, соответственно блокируем
            log.warn("При обновлении access обнаружено несколько активных сессий для пользователя {}. Все сессии будут заблокированы.", session.getUser().getUsername());
            blockUserSessions(session.getUser());
            throw new RuntimeException("Обнаружено несколько активных сессий. Все сессии заблокированы.");
        }

        // Проверка истечения refresh токена
        if (session.getRefreshTokenExpires().isBefore(LocalDateTime.now())) {
            log.info("Refresh токен истек, обновляем refresh токен для пользователя: {}", session.getUser().getUsername());
            // Обновляем refresh токен
            session.setRefreshToken(jwtUtil.generateRefreshToken(session.getUser()));
            session.setRefreshTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)));
        }

        // Обновляем access токен
        session.setAccessToken(jwtUtil.generateAccessToken(session.getUser()));
        session.setAccessTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(accessTokenExpiration)));
        session.setLastActivityTime(LocalDateTime.now());

        log.debug("Access токен обновлен для пользователя: {}", session.getUser().getUsername());

        return userSessionRepository.save(session);
    }

    /// Обновление refresh токена
    public UserSession refreshRefreshToken(String refreshToken){
        // Проверка на наличие активной сессии
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));

        if(session.getStatus() != STATUS.ACTIVE){
            throw new RuntimeException("Сессия не активна");
        }

        // Проверка на активные сессии
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(session.getUser(), STATUS.ACTIVE);
        if (activeSessions.size() > 1) {
            log.warn("Обнаружено несколько активных сессий для пользователя {}. Все сессии будут заблокированы.", session.getUser().getUsername());
            blockUserSessions(session.getUser());
            throw new RuntimeException("Обнаружено несколько активных сессий. Все сессии заблокированы.");
        }

        // В случае если сессия активна и нет подозрительной активности, обновим refresh токен текущей сессии (нет надобности создавать новую сессию)
        session.setRefreshToken(jwtUtil.generateRefreshToken(session.getUser()));
        session.setRefreshTokenExpires(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)));
        session.setLastActivityTime(LocalDateTime.now());

        log.debug("Refresh токен обновлен для пользователя: {}", session.getUser().getUsername());

        return userSessionRepository.save(session);
    }

    /// Блокировка активных сессий пользователя
    public void blockUserSessions(User user) {
        List<UserSession> sessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);
        sessions.forEach(session -> session.setStatus(STATUS.BLOCKED));

        log.warn("Все активные сессии пользователя {} заблокированы.", user.getUsername());
        userSessionRepository.saveAll(sessions);

        try{
            // Уведомление администраторам
            List<User> admins = userRepository.findByRole(ROLE.ROLE_ADMIN);
            log.debug("Получены администраторы для оповещения: {}", admins);
            for (User admin : admins) {
                registryUtil.sendNotification(admin.getEmail(), "Обнаружена подозрительная активность пользователя " + user.getUsername() +", его сессии были заблокированы");
            }
        } catch (Exception e){
            log.warn("Администраторы не найдены");
        }
    }

    /// Наличие заблокированных сессий
    public boolean hasBlockedSessions(User user) {
        List<UserSession> blockedSessions = userSessionRepository.findByUserAndStatus(user, STATUS.BLOCKED);
        return !blockedSessions.isEmpty();
    }

    /// Деактивация сессии по access токену
    public void deactivateSessionByAccessToken(String accessToken) {
        UserSession session = userSessionRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));

        // Лишний раз проверяем на активность
        if (session.getStatus() != STATUS.ACTIVE) {
            throw new RuntimeException("Сессия уже неактивна");
        }

        session.setStatus(STATUS.INACTIVE);
        session.setLastActivityTime(LocalDateTime.now());

        log.debug("Сессия с accessToken {} деактивирована для пользователя {}", accessToken, session.getUser().getUsername());

        userSessionRepository.save(session);
    }

    /// Поиск сессии по access токену
    public UserSession getSessionByAccessToken(String accessToken) {
        return userSessionRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));
    }

    /// Пометка неактивных сессий на отзыв
    public void markSessionsAsRevoked() {
        // Порог - 1 День
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<UserSession> inactiveSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.INACTIVE, threshold);

        if(!inactiveSessions.isEmpty()){
            log.debug("Найдено {} неактивных сессий для пометки на отзыв", inactiveSessions.size());

            inactiveSessions.forEach(session -> {
                session.setStatus(STATUS.REVOKED);
                session.setLastActivityTime(LocalDateTime.now());
            });

            userSessionRepository.saveAll(inactiveSessions);
            log.debug("{} сессий были помечены как REVOKED", inactiveSessions.size());
        } else {
            log.debug("Неактивных сессий для пометки на отзыв не найдено");
        }
    }

    /// Удаление сессий, помеченных на отзыв
    public void deleteRevokedSessions(){
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        List<UserSession> revokedSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.REVOKED, threshold);

        if(!revokedSessions.isEmpty()){
            log.info("Найдено {} сессий для удаления", revokedSessions.size());
            userSessionRepository.deleteAll(revokedSessions);
            log.debug("{} сессий было удалено", revokedSessions.size());
        } else {
            log.debug("Сессий для удаления не найдено");
        }
    }

    public void handleInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<UserSession> activeSessions = userSessionRepository.findByStatusAndLastActivityTimeBefore(STATUS.ACTIVE, threshold);

        if (!activeSessions.isEmpty()) {
            log.info("Найдено {} активных сессий для перевода в статус INACTIVE", activeSessions.size());

            activeSessions.forEach(session -> {
                session.setStatus(STATUS.INACTIVE);
                session.setLastActivityTime(LocalDateTime.now()); // По это сути имитация логаута пользователя, поэтому установим LAT
            });

            userSessionRepository.saveAll(activeSessions);
            log.info("Активные сессии успешно переведены в статус INACTIVE");
        } else {
            log.debug("Активных сессий для перевода в статус INACTIVE не найдено");
        }
    }

    /// Обработка INACTIVE и REVOKED сессий
    public void handlingInactiveAndRevokedSessions(){
        markSessionsAsRevoked();
        deleteRevokedSessions();
    }

    /// Обновление времени последней активности для активной сессии пользователя
    public void updateLastActivityTime(User user) {
        // Поиск активной сессии пользователя
        List<UserSession> activeSessions = userSessionRepository.findByUserAndStatus(user, STATUS.ACTIVE);

        if (activeSessions.isEmpty()) {
            log.warn("Активная сессия для пользователя {} не найдена.", user.getUsername());
            throw new RuntimeException("Активная сессия не найдена");
        }

        UserSession session = activeSessions.getFirst();

        session.setLastActivityTime(LocalDateTime.now());

        userSessionRepository.save(session);

        log.debug("Время последней активности обновлено для пользователя: {}", user.getUsername());
    }

    /// Сохранение сессии (для UserService)
    public void save(UserSession session) {
        userSessionRepository.save(session);
    }

    /// Поиск по пользователю и статусу (тоже для UserService)
    public List<UserSession> findByUserAndStatus(User user, STATUS status){
        return userSessionRepository.findByUserAndStatus(user, status);
    }

}
