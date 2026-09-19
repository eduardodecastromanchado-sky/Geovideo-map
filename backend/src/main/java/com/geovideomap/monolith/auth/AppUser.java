package com.geovideomap.monolith.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider; // "local" | "google"

    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "enabled", nullable = false)
    private int enabled = 1;

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_edicion")
    @UpdateTimestamp
    private LocalDateTime fechaEdicion;
}
