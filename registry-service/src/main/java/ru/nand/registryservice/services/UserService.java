package ru.nand.registryservice.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.entities.DTO.AccountPatchDTO;
import ru.nand.registryservice.repositories.UserRepository;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void save(User user) {
        log.debug("Сохраняем пользователя в БД: {}", user);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь с именем " + username + " не найден");
        }
        return user;
    }

    public int getFollowersCount(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Пользователь с именем " + username + " не найден");
        }
        return user.getSubscribers().size();
    }

    public List<String> getFollowers(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Пользователь с именем " + username + " не найден");
        }
        return user.getSubscribers().stream()
                .map(User::getUsername)
                .toList();
    }

    public List<String> getFollowing(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Пользователь с именем " + username + " не найден");
        }
        return user.getSubscriptions().stream()
                .map(User::getUsername)
                .toList();
    }

    public void followUser(String currentUsername, String targetUsername) {
        User currentUser = userRepository.findByUsername(currentUsername);
        User targetUser = userRepository.findByUsername(targetUsername);

        if (currentUser == null || targetUser == null) {
            throw new RuntimeException("Один из пользователей не найден");
        }

        if (currentUser.getSubscriptions().contains(targetUser)) {
            throw new RuntimeException("Пользователь уже подписан");
        }

        currentUser.getSubscriptions().add(targetUser);
        targetUser.getSubscribers().add(currentUser);

        userRepository.save(currentUser);
        userRepository.save(targetUser);
        log.debug("Пользователь {} подписался на {}", currentUsername, targetUsername);
    }

    public List<String> getAllUsernames() {
        return userRepository.findAll().stream()
                .map(User::getUsername)
                .toList();
    }

    // Обновляет данные пользователя
    public String updateUser(AccountPatchDTO accountPatchDTO) throws RuntimeException {
        User user = userRepository.findByUsername(accountPatchDTO.getFirstUsername());
        if (user == null) {
            throw new RuntimeException("Пользователь не найден");
        }

        if (accountPatchDTO.getUsername() != null && !accountPatchDTO.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(accountPatchDTO.getUsername()) != null) {
                throw new RuntimeException("Username уже используется");
            }
            user.setUsername(accountPatchDTO.getUsername());
        }

        if (accountPatchDTO.getEmail() != null && !accountPatchDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(accountPatchDTO.getEmail()) != null) {
                throw new RuntimeException("Email уже используется");
            }
            user.setEmail(accountPatchDTO.getEmail());
        }

        if (accountPatchDTO.getPassword() != null) {
            user.setPassword(accountPatchDTO.getPassword());
        }

        // TODO без jwtUtil чтобы не было цикличной зависимости
        String newJwt = generateToken(user);

        userRepository.save(user);
        log.debug("Данные пользователя обновлены: {}", user);

        return newJwt;
    }

    public void deleteUser(String username) {
        userRepository.deleteByUsername(username);
    }





    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        User user = userRepository.findByUsername(userDetails.getUsername());

        claims.put("role", user.getRole().name());
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}

