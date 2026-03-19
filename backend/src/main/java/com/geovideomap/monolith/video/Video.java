package com.geovideomap.monolith.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "youtube_videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String title;

    @Column(name = "descripcion", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "id_youtube")
    private String youtubeId;

    @Column(name = "latitude", precision = 10, scale = 8)
    private Double latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private VideoStatus status;

    @Column(name = "trip_id") // This column will be added by hibernate
    private String tripId;

    @Column(name = "trip_order") // This column will be added by hibernate
    private Integer tripOrder;

    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "fecha_edicion")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Note: Other fields from DDL like 'gancho', 'visitas', etc., are omitted for now for simplicity.
}
