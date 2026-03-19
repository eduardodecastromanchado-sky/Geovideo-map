package com.geovideomap.monolith.video;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoRepository videoRepository;

    public VideoController(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Video>> getVideos(
            Pageable pageable,
            @RequestParam(required = false) String tripId) {
        
        Page<Video> videos;
        if (tripId != null && !tripId.isEmpty()) {
            videos = videoRepository.findByTripId(tripId, pageable);
        } else {
            videos = videoRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Temporary endpoint to verify database connection and data mapping.
     * Fetches the first 5 videos from the database.
     */
    @GetMapping("/test-db")
    public ResponseEntity<List<Video>> testDatabaseConnection() {
        // Fetch the first 5 videos
        Page<Video> videoPage = videoRepository.findAll(PageRequest.of(0, 5));
        // In a real scenario, this would be logged. Returning it allows verification.
        return ResponseEntity.ok(videoPage.getContent());
    }
}
