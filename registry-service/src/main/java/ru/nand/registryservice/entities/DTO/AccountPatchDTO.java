package ru.nand.registryservice.entities.DTO;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountPatchDTO {

    private String firstUsername; // Это username пользователя, который хочет обновить данные аккаунта (пригодится если пользователь будет менять username)

    private String username;

    @Email(message = "Почта должна быть в верном формате")
    private String email;

    private String password;
}
