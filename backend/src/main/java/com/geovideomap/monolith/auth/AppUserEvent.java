package com.geovideomap.monolith.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "app_user_events")
public class AppUserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // FK nullable

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType; // REGISTER | LOGIN_OK | LOGIN_FAIL | LOGIN_GOOGLE | LOGOUT | DISABLED | ENABLED | PROFILE_UPDATE

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
