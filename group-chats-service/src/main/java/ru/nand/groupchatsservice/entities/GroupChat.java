package ru.nand.groupchatsservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("group_chats")
public class GroupChat {
    @PrimaryKey
    private UUID groupChatId;

    private String groupName;

    private int creatorId;

    private LocalDateTime createdAt;
}
