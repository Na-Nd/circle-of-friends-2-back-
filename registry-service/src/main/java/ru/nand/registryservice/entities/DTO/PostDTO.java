package ru.nand.registryservice.entities.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private String text;
    private Set<String> tags;
    private String author;
    private String filename;
    private String imageBase64;
    private int likesCount;
    private int commentsCount;

    @Override
    public String toString() {
        return "PostDTO{" +
                "text='" + text + '\'' +
                ", tags=" + tags +
                ", author='" + author + '\'' +
                ", filename='" + filename + '\'' +
                ", imageBase64='" + imageBase64 + '\'' +
                ", likesCount=" + likesCount +
                ", commentsCount=" + commentsCount +
                '}';
    }
}
