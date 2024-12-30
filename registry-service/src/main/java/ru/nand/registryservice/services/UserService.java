package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.MyUserRepository;
import ru.nand.sharedthings.DTO.AccountPatchDTO;

import java.util.List;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final MyUserRepository userRepository;

    @Autowired
    public UserService(MyUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void save(User user){
        log.debug("Сохраняем пользователя в БД: {}", user.toString());
        userRepository.save(user);
    }

    public User findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    public User findByEmail(String email){
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
        int count = user.getSubscribers().size();
        log.debug("Количество подписчиков пользователя {}: {}", username, count);
        return count;
    }

    public List<String> getAllUsernames() {
        List<User> users = userRepository.findAll();
        List<String> usernames = users.stream()
                .map(User::getUsername)
                .toList();
        log.debug("Список пользователей: {}", usernames);
        return usernames;
    }

    public void updateUser(AccountPatchDTO accountPatchDTO) throws RuntimeException{
        String firstUsername = accountPatchDTO.getFirstUsername();

        User user = userRepository.findByUsername(firstUsername);

        // Если пользователь изменил username
        if(!user.getUsername().equals(accountPatchDTO.getUsername())){
            // Если null - значит оставить старый username
            if(accountPatchDTO.getUsername() == null){
                user.setUsername(firstUsername);
            } else if(userRepository.findByUsername(accountPatchDTO.getUsername()) == null){ // В противном случае меняем на новый при том условии, что таких username'ов еще нет
                user.setUsername(accountPatchDTO.getUsername());
            } else {
                throw new RuntimeException("Не удалось обновить username: " + accountPatchDTO.getUsername() + " уже существует");
            }
        }

        // Если пользователь сменил email
        if(!user.getEmail().equals(accountPatchDTO.getEmail())){
            // Если null - оставить старый, в противном случае меняем если такого email'а еще нет
            if(accountPatchDTO.getEmail() != null){
                if(userRepository.findByEmail(accountPatchDTO.getEmail()) == null){
                    user.setEmail(accountPatchDTO.getEmail());
                } else {
                    throw new RuntimeException("Не удалось обновить email: " + accountPatchDTO.getEmail() + " уже существует");
                }
            }
        }

        // Если сменил пароль, сразу меняем если не null, в противном случае оставим старый
        if (accountPatchDTO.getPassword() != null){
            user.setPassword(accountPatchDTO.getPassword());
        }

        log.debug("Обновленный пользователь сохранен: {}", user.toString());

        userRepository.save(user);
    }

    public void deleteUser(String username){
        userRepository.deleteByUsername(username);
    }
}
