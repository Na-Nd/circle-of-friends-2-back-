package ru.nand.registryservice.entities.DTO.PostsUserService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentCreateDTO {
    private String commentText;
    private String commentAuthorUsername;
}
