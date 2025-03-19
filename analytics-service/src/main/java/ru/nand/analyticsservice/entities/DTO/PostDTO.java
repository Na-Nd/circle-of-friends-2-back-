package ru.nand.analyticsservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostDTO {
    private int postId;
    private String ownerUsername;
    private String text;
    private LocalDateTime dateOfPublication;
    private Set<String> tags;
    private int likes;
    private int comments;
    private List<String> imagesUrls;
    private List<byte[]> images;
    private List<String> imagesBase64;
}
