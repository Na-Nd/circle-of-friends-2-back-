package ru.nand.registryservice.entities.DTO.PostsUserService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentDTO {
    private int commentId;
    private int postId;
    private String commentAuthorUsername;
    private String text;
    private LocalDateTime creationDate;
}
