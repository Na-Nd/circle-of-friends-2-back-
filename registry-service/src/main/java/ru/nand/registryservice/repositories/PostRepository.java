package ru.nand.registryservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nand.registryservice.entities.Post;
import ru.nand.registryservice.entities.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByAuthor(User user);

    @Query("SELECT p FROM Post p WHERE (SELECT COUNT(t) FROM p.tags t WHERE t IN :tags) = :size") // JPQL
    List<Post> findByTagsIn(@Param("tags") List<String> tags, @Param("size") int size);

    @Query("SELECT p FROM Post p WHERE LOWER(p.text) LIKE LOWER(CONCAT(:text, '%'))")
    List<Post> findByTextStartingWith(@Param("text") String text);

    List<Post> findByAuthorIn(Set<User> authors);

    List<Post> findByDateOfPublicationBetween(LocalDateTime start, LocalDateTime end);
}
