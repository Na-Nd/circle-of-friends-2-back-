package ru.nand.registryservice.entities.DTO.AnalyticsService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nand.registryservice.entities.DTO.PostsUserService.PostDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatedPostsDTO {
    private long postsCount;

    private List<PostDTO> createdPosts;
}
