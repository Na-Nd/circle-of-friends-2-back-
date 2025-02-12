package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.RubySettingsOrBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.Comment;
import ru.nand.registryservice.entities.DTO.CommentDTO;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.DTO.PostDTO;
import ru.nand.registryservice.entities.Post;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.CommentRepository;
import ru.nand.registryservice.repositories.PostRepository;
import ru.nand.registryservice.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CommentRepository commentRepository;

    @Transactional
    public void createPost(PostDTO postDTO) {
        User author = userRepository.findByUsername(postDTO.getAuthor());

        //TODO modelmapper
        Post post = new Post();
        post.setText(postDTO.getText());
        post.setTags(postDTO.getTags());
        post.setAuthor(author);
        post.setDateOfPublication(LocalDateTime.now());
        post.setDateOfUpdate(LocalDateTime.now());
        post.setFilename(postDTO.getFilename());

        postRepository.save(post);
        log.debug("Сохранил пост: {}", post);

        Set<User> subscribers = author.getSubscribers();
        for(User subscriber : subscribers) {
            sendNotification(
                    subscriber.getEmail(),
                    author.getUsername(),
                    "Посмотрите новый пост от " + author.getUsername()
            );
            // Можно было бы сохранять уведомление сразу, но придется перепилить текущую реализацию отправки и сохранения уведомлений или сделать новую
            // Или отправить уведомление из микросервиса постов, но так получилось бы медленнее
        }
    }

    public PostDTO getPostById(int id) throws RuntimeException{
        Optional<Post> optionalPost = postRepository.findById(id);

        if(optionalPost.isEmpty()){
            throw new RuntimeException("Пост не найден");
        }

        Post post = optionalPost.get();

        return new PostDTO(
                post.getText(),
                post.getTags(),
                post.getAuthor().getUsername(),
                post.getFilename(),
                null
        );
    }

    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(post -> new PostDTO(
                        post.getText(),
                        post.getTags(),
                        post.getAuthor().getUsername(),
                        post.getFilename(),
                        null
                ))
                .collect(Collectors.toList());
    }

    public List<PostDTO> getPostsByAuthor(String author) {
        List<Post> posts = postRepository.findByAuthor(userRepository.findByUsername(author));
        return posts.stream()
                .map(post -> new PostDTO( // тоже modelmapper'ом можно
                        post.getText(),
                        post.getTags(),
                        post.getAuthor().getUsername(),
                        post.getFilename(),
                        null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePost(int postId, PostDTO postDTO) throws RuntimeException{
        Post existingPost = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Обновляем только переданные данные
        if(postDTO.getText() != null){
            existingPost.setText(postDTO.getText());
        }
        if(postDTO.getTags() != null){
            existingPost.setTags(postDTO.getTags());
        }
        if(postDTO.getFilename() != null){
            existingPost.setFilename(postDTO.getFilename());
        }

        postRepository.save(existingPost);
    }

    public List<PostDTO> getPostsByTags(List<String> tags) {
        return postRepository.findByTagsIn(tags, tags.size()).stream()
                .map(post -> new PostDTO(
                        post.getText(),
                        post.getTags(),
                        post.getAuthor().getUsername(),
                        post.getFilename(),
                        null)
                ).collect(Collectors.toList());
    }

    @Transactional
    public void deletePost(int postId){
        postRepository.deleteById(postId);
    }

    public List<PostDTO> getPostsByText(String text) {
        return postRepository.findByTextStartingWith(text).stream()
                .map(post -> new PostDTO(
                        post.getText(),
                        post.getTags(),
                        post.getAuthor().getUsername(),
                        post.getFilename(),
                        null)
                ).collect(Collectors.toList());
    }


    public List<PostDTO> getSubscriptionPosts(String username){
        User user = userRepository.findByUsername(username);

        Set<User> subscriptions = user.getSubscriptions();

        return postRepository.findByAuthorIn(subscriptions).stream()
                .map(post -> new PostDTO(
                        post.getText(),
                        post.getTags(),
                        post.getAuthor().getUsername(),
                        post.getFilename(),
                        null)
                ).collect(Collectors.toList());
    }

    // TODO Util
    private void sendNotification(String targetUserEmail, String authorUsername, String notificationMessage){
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setUserEmail(targetUserEmail);
        notificationDTO.setMessage(notificationMessage);

        try{
            String message = new ObjectMapper().writeValueAsString(notificationDTO);
            kafkaTemplate.send("user-notifications-topic", message);
            log.debug("Создано уведомление пользователю {} о новом посте автора {} и отправлено в брокер", targetUserEmail, authorUsername);
        } catch (JsonProcessingException e) {
            log.error("Ошибка при сериализации уведомления: {}", e.getMessage());
        }
    }

    @Transactional
    public void likePost(int postId, String username){
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));
        User user = userRepository.findByUsername(username);

        log.debug("Получен пост с id {} и пользователь с id {}", post.getId(), user.getId());

        post.getLikes().add(user);
        postRepository.save(post);
    }

    @Transactional
    public void unlikePost(int postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));
        User user = userRepository.findByUsername(username);

        post.getLikes().remove(user);
        postRepository.save(post);
    }

    public int getLikesCount(int postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));
        return post.getLikes().size();
    }

    public List<String> getLikedUsers(int postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));
        return post.getLikes().stream().map(User::getUsername).collect(Collectors.toList());
    }

    @Transactional
    public void addComment(int postId, CommentDTO commentDTO){
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Пост не найден"));
        User user = userRepository.findByUsername(commentDTO.getUsername());

        if(user == null) {
            throw new RuntimeException("Пользователь не найден");
        }

        // TODO modelMapper
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setText(commentDTO.getText());
        comment.setDateOfCreation(LocalDateTime.now());

        commentRepository.save(comment);
    }

    @Transactional
    public void editComment(int commentId, CommentDTO commentDTO){
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        if(!comment.getAuthor().getUsername().equals(commentDTO.getUsername())){
            throw new RuntimeException("Доступ к редактированию чужого поста запрещен");
        }

        comment.setText(commentDTO.getText());

        commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(int commentId, String username){
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        if(!comment.getAuthor().getUsername().equals(username)){
            throw new RuntimeException("Доступ к удалению чужого поста запрещен");
        }

        commentRepository.delete(comment);
    }

    public List<CommentDTO> getComments(int postId, int page, int size){
        Pageable pageable = PageRequest.of(page, size);

        return commentRepository.findByPostId(postId, pageable).stream()
                .map(comment -> new CommentDTO(
                        comment.getAuthor().getUsername(),
                        comment.getText())
                ).collect(Collectors.toList());
    }

}
