package ru.nand.registryservice.entities.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    @NotBlank(message = "Имя пользователя не должно быть пустое")
    private String username;

    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;

    private String requestId;
}