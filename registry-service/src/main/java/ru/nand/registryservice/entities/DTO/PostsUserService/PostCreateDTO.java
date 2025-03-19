package ru.nand.registryservice.entities.DTO.PostsUserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateDTO {
    private String text;
    private Set<String> tags;
    private List<String> imagesUrls; // Список файлов (картинок)
    private String ownerUsername;
}
