package com.geovideomap.monolith.video;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Capa de servicio para el módulo de vídeos.
 *
 * El controlador solo habla con este servicio; el repositorio JPA queda
 * encapsulado aquí. Facilita tests unitarios (Mockito) y extensiones futuras
 * (filtros, proyecciones, caché) sin tocar el controlador.
 */
@Service
public class VideoService {

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    /**
     * Devuelve una página de vídeos con el Pageable que llega desde el cliente.
     * Comportamiento idéntico al que tenía el controlador directamente.
     */
    public Page<Video> list(Pageable pageable) {
        return videoRepository.findAll(pageable);
    }
}
