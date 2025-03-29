package ru.nand.adminservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private int id;

    private String username;

    private String email;

    private int subscribersCount;

    private int subscriptionsCount;

    private int postsCount;
}