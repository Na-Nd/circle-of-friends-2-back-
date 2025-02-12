package ru.nand.postsuserservice.entities.requests;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class PostUpdateRequest {
    private String text;
    private Set<String> tags;
    private MultipartFile image;
}
