package ru.nand.registryservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.DTO.UserDTO;
import ru.nand.registryservice.entities.Post;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.PostRepository;
import ru.nand.registryservice.repositories.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserSessionService userSessionService;

    /// Установка лайка на пост
    @Transactional
    public String likePost(int postId, String username){
        try{
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            userSessionService.updateLastActivityTime(user);

            post.getLikes().add(user);

            postRepository.save(post);

            return "Лайк к посту успешно добавлен";
        } catch (Exception e){
            log.warn("Ошибка при лайке поста с id {} пользователем {}: {}", postId, username, e.getMessage());
            throw new RuntimeException("Ошибка при лайке поста с id " + postId + " пользователем " + username + ": " + e.getMessage());
        }
    }

    /// Снятие лайка с поста
    @Transactional
    public String unlikePost(int postId, String username){
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            userSessionService.updateLastActivityTime(user);

            post.getLikes().remove(user);

            postRepository.save(post);

            return "Лайк с поста успешно снят";
        } catch (Exception e){
            log.warn("Ошибка при снятии лайка с поста с id {} пользователем {}: {}", postId, username, e.getMessage());
            throw new RuntimeException("Ошибка при снятии лайка с поста с id " + postId + " пользователем " + username + ": " + e.getMessage());
        }
    }

    /// Получение списка пользователей, лайкнувших пост
    public List<UserDTO> getLikedUsers(int postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        return post.getLikes().stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getSubscribers().size(),
                        user.getSubscriptions().size(),
                        user.getPosts().size()
                )).collect(Collectors.toList());
    }
}
