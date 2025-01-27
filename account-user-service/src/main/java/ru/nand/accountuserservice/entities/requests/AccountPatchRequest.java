package ru.nand.accountuserservice.entities.requests;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountPatchRequest {
    private String username;

    @Email(message = "Почта должна быть в верном формате")
    private String email;

    private String password;

    @Override
    public String toString() {
        return "AccountPatchRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
