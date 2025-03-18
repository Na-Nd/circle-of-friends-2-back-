package ru.nand.groupchatsservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupChatDTO {
    private UUID groupChatId;

    private String groupChatName;

    private Set<Integer> participants;
}
