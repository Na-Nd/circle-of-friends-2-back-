package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.MyUserRepository;

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
}
