package ru.nand.registryservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostUpdateDTO {
    private String text;
    private Set<String> tags;
    private List<String> imagesUrls; // Список файлов (картинок)
    private String ownerUsername;
}