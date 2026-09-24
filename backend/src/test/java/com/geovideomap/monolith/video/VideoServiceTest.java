package com.geovideomap.monolith.video;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario de VideoService.
 * Sin Spring context, sin DB.
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock VideoRepository videoRepository;
    @InjectMocks VideoService videoService;

    @Test
    @DisplayName("list() delega en videoRepository.findAll y retorna la página")
    void list_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Video v = new Video();
        v.setId(1);
        v.setTitle("Sky Drift");
        Page<Video> expected = new PageImpl<>(List.of(v), pageable, 1);

        when(videoRepository.findAll(pageable)).thenReturn(expected);

        Page<Video> result = videoService.list(pageable);

        assertThat(result).isSameAs(expected);
        verify(videoRepository).findAll(pageable);
    }
}
