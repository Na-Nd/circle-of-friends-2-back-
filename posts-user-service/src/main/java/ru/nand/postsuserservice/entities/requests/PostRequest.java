package ru.nand.postsuserservice.entities.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    private String text;

    private Set<String> tags;

    private String author;

   private MultipartFile image; // Картинка

}
