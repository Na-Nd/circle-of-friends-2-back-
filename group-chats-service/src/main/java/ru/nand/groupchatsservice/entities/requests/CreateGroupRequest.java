package ru.nand.groupchatsservice.entities.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRequest {
    @NotEmpty(message = "Список пользователей не должен быть пустым")
    private Set<Integer> usersIds;

    @NotBlank(message = "Название группы не должно быть пустым")
    private String groupName;
}