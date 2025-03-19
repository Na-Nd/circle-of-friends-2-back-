package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.DTO.AnalyticsService.CreatedPostsDTO;
import ru.nand.registryservice.entities.DTO.PostsUserService.PostCreateDTO;
import ru.nand.registryservice.entities.DTO.PostsUserService.PostDTO;
import ru.nand.registryservice.entities.DTO.PostsUserService.PostUpdateDTO;
import ru.nand.registryservice.entities.Post;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.PostRepository;
import ru.nand.registryservice.repositories.UserRepository;
import ru.nand.registryservice.utils.RegistryUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RegistryUtil registryUtil;
    private final ObjectMapper objectMapper;
    private final UserSessionService userSessionService;

    /// Создание поста
    @Transactional
    public void createPost(String requestMessage){
        PostCreateDTO postCreateDTO;
        try{
            postCreateDTO = objectMapper.readValue(requestMessage, PostCreateDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при десериализации данных: {}", e.getMessage());
            throw new RuntimeException("Ошибка при десериализации данных: " + e.getMessage());
        }

        User postAuthor = userRepository.findByUsername(postCreateDTO.getOwnerUsername())
                .orElseThrow(() -> new RuntimeException("Автор поста не найден"));

        Post post = Post.builder()
                .text(postCreateDTO.getText())
                .tags(postCreateDTO.getTags())
                .author(postAuthor)
                .dateOfPublication(LocalDateTime.now())
                .dateOfUpdate(LocalDateTime.now())
                .filenames(postCreateDTO.getImagesUrls())
                .build();

        postRepository.save(post);
        log.debug("Сохранил пост автора: {}", postAuthor.getUsername());

        // Отправка уведомлений подписчикам
        for (User subscriber : postAuthor.getSubscribers()) {
            registryUtil.sendNotification(
                    subscriber.getEmail(),
                    "Посмотрите новый пост от " + postAuthor.getUsername()
            );
        }

        // Обновление последней активности автора
        userSessionService.updateLastActivityTime(postAuthor);
    }

    /// Получение поста по id
    public String getPostById(int id){
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        PostDTO postDTO = PostDTO.builder()
                .postId(post.getId())
                .ownerUsername(post.getAuthor().getUsername())
                .text(post.getText())
                .dateOfPublication(post.getDateOfPublication())
                .tags(post.getTags())
                .likes(post.getLikes().size())
                .comments(post.getComments().size())
                .imagesUrls(post.getFilenames())
                .images(null)
                .build();

        try{
            String responseMessage = objectMapper.writeValueAsString(postDTO);
            return responseMessage;
        } catch (JsonProcessingException e){
            log.warn("Ошибка при сериализации PostDTO в JSON: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации PostDTO в JSON: " + e.getMessage());
        }

    }

    /// Удаление поста по id
    @Transactional
    public String deletePost(int id, String ownerUsername){
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пост с id " + id + " не найден"));

        if(!post.getAuthor().getUsername().equals(ownerUsername)){
            throw new RuntimeException("Пользователь" + ownerUsername + " не может удалить чужой пост");
        }

        String responseMessage;
        try{
            responseMessage = objectMapper.writeValueAsString(post.getFilenames());
        } catch (Exception e){
            throw new RuntimeException("Ошибка сериализации сообщения ответа");
        }

        // Достаем пользователя для обновления последней активности
        User user = post.getAuthor();
        userSessionService.updateLastActivityTime(user);

        log.debug("Пост с id {} удален", id);
        postRepository.delete(post);

        // Возвращаем список названий изображений для удаления
        return responseMessage;
    }

    /// Редактирование поста по id (PUT)
    @Transactional
    public String updatePost(int postId, String requestMessage){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Разбираем JSON в DTO
        PostUpdateDTO postUpdateDTO;
        try{
            postUpdateDTO = objectMapper.readValue(requestMessage, PostUpdateDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при десериализации данных : {}", e.getMessage());
            throw new RuntimeException("Ошибка при десериализации данных: " + e.getMessage());
        }

        // Перезаписываем данные
        post.setText(postUpdateDTO.getText());
        post.setTags(postUpdateDTO.getTags());
        post.setFilenames(postUpdateDTO.getImagesUrls());

        // Достаем пользователя для обновления последней активности
        User user = post.getAuthor();
        userSessionService.updateLastActivityTime(user);

        postRepository.save(post);

        return "Пост успешно обновлен";
    }

    /// Получение всех постов
    public List<PostDTO> getAllPosts(){
        return postRepository.findAll().stream()
                .map(post -> new PostDTO(
                        post.getId(),
                        post.getAuthor().getUsername(),
                        post.getText(),
                        post.getDateOfPublication(),
                        post.getTags(),
                        post.getLikes().size(),
                        post.getComments().size(),
                        post.getFilenames(),
                        null,
                        null
                )).collect(Collectors.toList());
    }

    /// Получение постов конкретного пользователя
    public List<PostDTO> getPostsByAuthor(String authorUsername){
        return postRepository.findByAuthor(
                userRepository.findByUsername(authorUsername).orElseThrow(()-> new RuntimeException("Пользователь не найден"))
                ).stream()
                .map(post -> new PostDTO(
                        post.getId(),
                        authorUsername,
                        post.getText(),
                        post.getDateOfPublication(),
                        post.getTags(),
                        post.getLikes().size(),
                        post.getComments().size(),
                        post.getFilenames(),
                        null,
                        null
                )).collect(Collectors.toList());
    }

    /// Получение постов подписок конкретного пользователя
    public List<PostDTO> getSubscriptionsPosts(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Set<User> subscriptions = user.getSubscriptions();

        return postRepository.findByAuthorIn(subscriptions).stream()
                .map(post -> new PostDTO(
                        post.getId(),
                        post.getAuthor().getUsername(),
                        post.getText(),
                        post.getDateOfPublication(),
                        post.getTags(),
                        post.getLikes().size(),
                        post.getComments().size(),
                        post.getFilenames(),
                        null,
                        null
                )).collect(Collectors.toList());
    }

    /// Получение постов по тэгам
    public List<PostDTO> getPostsByTags(List<String> tags){
        return postRepository.findByTagsIn(tags, tags.size()).stream()
                .map(post -> new PostDTO(
                        post.getId(),
                        post.getAuthor().getUsername(),
                        post.getText(),
                        post.getDateOfPublication(),
                        post.getTags(),
                        post.getLikes().size(),
                        post.getComments().size(),
                        post.getFilenames(),
                        null,
                        null
                )).collect(Collectors.toList());
    }

    /// Получение поста по тексту
    public List<PostDTO> getPostsByText(String text){
        return postRepository.findByTextStartingWith(text).stream()
                .map(post -> new PostDTO(
                        post.getId(),
                        post.getAuthor().getUsername(),
                        post.getText(),
                        post.getDateOfPublication(),
                        post.getTags(),
                        post.getLikes().size(),
                        post.getComments().size(),
                        post.getFilenames(),
                        null,
                        null
                )).collect(Collectors.toList());
    }

    /// Получение созданных постов за последние N часов
    public String getCreatedPosts(int hoursCount) {
        try {
            // Получаем текущее время и время, которое было N часов назад
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusHours(hoursCount);

            // Получаем список постов, созданных за последние N часов
            List<Post> posts = postRepository.findByDateOfPublicationBetween(startTime, now);

            List<PostDTO> postDTOs = posts.stream()
                    .map(post -> PostDTO.builder()
                            .postId(post.getId())
                            .ownerUsername(post.getAuthor().getUsername())
                            .text(post.getText())
                            .dateOfPublication(post.getDateOfPublication())
                            .tags(post.getTags())
                            .likes(post.getLikes().size())
                            .comments(post.getComments().size())
                            .imagesUrls(post.getFilenames())
                            .images(null) // Для аналитики нет смысла обращаться к микросервису постов и брать от туда Base64 изображений или их байты
                            .imagesBase64(null) // Для аналитики нет смысла обращаться к микросервису постов и брать от туда Base64 изображений или их байты
                            .build())
                    .collect(Collectors.toList());

            CreatedPostsDTO createdPostsDTO = CreatedPostsDTO.builder()
                    .postsCount(postDTOs.size())
                    .createdPosts(postDTOs)
                    .build();

            return objectMapper.writeValueAsString(createdPostsDTO);
        } catch (Exception e) {
            log.warn("Ошибка при получении постов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении постов: "+ e.getMessage());
        }
    }
}
