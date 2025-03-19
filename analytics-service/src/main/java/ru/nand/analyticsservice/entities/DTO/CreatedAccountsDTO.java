package ru.nand.analyticsservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatedAccountsDTO {
    private long accountsCount;

    private List<UserDTO> createdAccounts;
}
