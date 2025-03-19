package ru.nand.registryservice.entities.DTO.AnalyticsService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nand.registryservice.entities.DTO.UserDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatedAccountsDTO {
    private long accountsCount;

    private List<UserDTO> createdAccounts;
}
