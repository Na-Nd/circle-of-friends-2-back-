package ru.nand.registryservice.entities.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {

    @NotBlank(message = "Имя пользователя не должно быть пустым")
    private String username;

    @Email(message = "Почта должна быть в верном формате")
    @NotBlank(message = "Почта не должна быть пустой")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    private String password;

    private String requestId;
}
