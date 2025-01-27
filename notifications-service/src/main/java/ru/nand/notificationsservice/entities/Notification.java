package ru.nand.notificationsservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    private int id;

    private String ownerUsername;

    private String text;

    private LocalDateTime creationDate;

}
