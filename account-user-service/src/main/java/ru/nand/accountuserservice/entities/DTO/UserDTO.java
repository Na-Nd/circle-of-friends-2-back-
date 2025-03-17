package ru.nand.accountuserservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {
    private int id;

    private String username;

    private String email;

    private int subscribersCount;

    private int subscriptionsCount;

    private int postsCount;
}
