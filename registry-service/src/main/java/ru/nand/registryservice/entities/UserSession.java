package ru.nand.registryservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ru.nand.registryservice.entities.ENUMS.STATUS;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime accessTokenExpires;

    private LocalDateTime refreshTokenExpires;

    private LocalDateTime sessionCreationTime;

    private LocalDateTime lastActivityTime;

    @Enumerated(EnumType.STRING)
    private STATUS status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
