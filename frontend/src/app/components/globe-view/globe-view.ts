import { Component, AfterViewInit, ViewChild, ElementRef, Input } from '@angular/core';
import { VideoApiService, Page, Video } from '../../services/video-api';

declare const Cesium: any; // Use 'any' to avoid TypeScript errors for the global Cesium object

import { VideoInfoPanelComponent } from '../video-info-panel/video-info-panel.component';

@Component({
  selector: 'app-globe-view',
  templateUrl: './globe-view.html',
  styleUrls: ['./globe-view.scss'],
  standalone: true, // Mark this component as standalone
  imports: [VideoInfoPanelComponent] // Import the panel component here
})
export class GlobeViewComponent implements AfterViewInit {

  @ViewChild('cesiumContainer') cesiumContainer!: ElementRef;
  private viewer: any;
  private handler: any;

  selectedVideo: Video | null = null;
  isInfoPanelVisible: boolean = false;

  private lastHighlighted: any = null;
  private defaultPointStyle = {
    pixelSize: 8,
    color: Cesium.Color.DEEPSKYBLUE,
    outlineColor: Cesium.Color.WHITE,
    outlineWidth: 2
  };
  private highlightedPointStyle = {
    pixelSize: 12,
    color: Cesium.Color.AQUA,
    outlineColor: Cesium.Color.WHITE,
    outlineWidth: 3
  };

  constructor(private videoApiService: VideoApiService) { }

  ngAfterViewInit(): void {
    if (this.cesiumContainer) {
      this.viewer = new Cesium.Viewer(this.cesiumContainer.nativeElement, {
        animation: false,
        timeline: false,
        homeButton: false,
        geocoder: false,
        navigationHelpButton: false,
        baseLayerPicker: false,
        sceneModePicker: false,
        infoBox: false // Disable default InfoBox
      });

      // Allow iframes in the InfoBox for YouTube embeds
      this.viewer.infoBox.frame.setAttribute('sandbox', 'allow-same-origin allow-scripts allow-popups allow-forms');
      
      this.fetchAndDisplayVideos();
      this.setupEntityInteraction();
    }
  }

  fetchAndDisplayVideos(): void {
    this.videoApiService.getVideos(0, 50).subscribe({
      next: (videoPage: Page<Video>) => {
        this.addVideoMarkers(videoPage.content);
        this.drawTripLines(videoPage.content);
      },
      error: (err) => console.error('Error fetching videos:', err)
    });
  }

  addVideoMarkers(videos: Video[]): void {
    if (!this.viewer) return;
    videos.forEach(video => {
      if (video.latitude !== undefined && video.longitude !== undefined) {
        // Using the iframe embed for the description
        const descriptionHtml = `
          <div style="padding:10px; font-family: sans-serif;">
            <h4>${video.title}</h4>
            <iframe 
              width="100%" 
              height="200" 
              src="https://www.youtube.com/embed/${video.youtubeId}" 
              frameborder="0" 
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
              allowfullscreen>
            </iframe>
            <p>${video.description || 'No description.'}</p>
          </div>
        `;

        const entity = this.viewer.entities.add({
          id: video.id,
          name: video.title,
          position: Cesium.Cartesian3.fromDegrees(video.longitude, video.latitude),
          point: this.defaultPointStyle,
          video: video, // Attach video data to the entity
          description: descriptionHtml // This will now be used by our custom panel
        });
      }
    });
  }

  drawTripLines(videos: Video[]): void {
    // ... this method remains the same
  }

  setupEntityInteraction(): void {
    // ... this method remains the same
  }

  closePanel(): void {
    // ... this method remains the same
  }
}
