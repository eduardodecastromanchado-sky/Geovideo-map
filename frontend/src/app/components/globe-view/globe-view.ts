import { Component, AfterViewInit, ViewChild, ElementRef, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VideoApiService, Page, Video } from '../../services/video-api';
import { VideoInfoPanelComponent } from '../video-info-panel/video-info-panel';

declare const Cesium: any;

@Component({
  selector: 'app-globe-view',
  templateUrl: './globe-view.html',
  styleUrls: ['./globe-view.scss'],
  standalone: true,
  imports: [VideoInfoPanelComponent, CommonModule],
  styles: [`
    :host { 
      display: block; 
      position: relative; 
      width: 100%; 
      height: 100vh; 
    }
  `]
})
export class GlobeViewComponent implements AfterViewInit {

  @ViewChild('cesiumContainer') cesiumContainer!: ElementRef;
  private viewer: any;
  private handler: any;

  selectedVideo: Video | null = null;
  isInfoPanelVisible: boolean = false;

  private lastHighlighted: any = null;
  private defaultPointStyle: any;
  private highlightedPointStyle: any;

  constructor(
    private videoApiService: VideoApiService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) { }

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
        infoBox: false,
        selectionIndicator: false,
        requestRenderMode: true,
        maximumRenderTimeChange: Infinity,
        // Usamos ArcGIS World Imagery como base SATÉLITE DIRECTO
        imageryProvider: new Cesium.ArcGisMapServerImageryProvider({
          url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
        })
      });

      // 1. ELIMINAR TINTE AZUL Y OPTIMIZAR PARA ALTA RESOLUCIÓN
      const scene = this.viewer.scene;
      const globe = scene.globe;

      globe.baseColor = Cesium.Color.BLACK; // Fondo neutro para que no tiña el satélite
      globe.showGroundAtmosphere = false; // ELIMINA EL HALO AZUL SOBRE EL MAPA
      scene.skyAtmosphere.show = false; // ELIMINA EL CIELO AZUL QUE TIÑE LOS BORDES
      scene.fog.enabled = false; // ELIMINA LA NIEBLA QUE ACLARA/AZULEA EL FONDO
      
      // 2. AJUSTES TÉCNICOS PARA PANTALLAS DE ALTA RESOLUCIÓN (Android 3216x1440)
      this.viewer.scene.logarithmicDepthBuffer = false; 
      
      // Detección de dispositivo móvil mejorada
      const isMobile = /Mobi|Android/i.test(navigator.userAgent);
      if (isMobile) {
        console.log('High-res mobile detected. Balancing quality/performance.');
        // Para pantallas tan altas como 3216px, un 0.7 es mucho. Bajamos un pelín más o mantenemos según fluidez.
        this.viewer.resolutionScale = 0.8; // Mayor calidad para esas resoluciones nuevas
        // Optimizar carga de tiles para que no tarde el satélite
        this.viewer.scene.globe.maximumScreenSpaceError = 2; 
      } else {
        this.viewer.resolutionScale = 1.0;
        this.viewer.scene.globe.maximumScreenSpaceError = 1.5;
      }

      // Optimización Móvil y Rendimiento
      const isMobile = /Mobi|Android/i.test(navigator.userAgent);
      if (isMobile) {
        console.log('Mobile device detected. Optimizing Cesium performance.');
        this.viewer.resolutionScale = 0.7; // Reducir escala de resolución para ganar fluidez
        this.viewer.scene.debugShowFramesPerSecond = false;
        this.viewer.scene.fog.enabled = false; // Deshabilitar niebla para ahorrar GPU
      }

      // Posicionamiento inicial de la cámara para que no se vea vacío el mundo
      this.viewer.camera.setView({
        destination: Cesium.Cartesian3.fromDegrees(-70, 18, 15000000), // Ver América/Caribe desde el espacio a altura razonable
        orientation: {
          heading: 0,
          pitch: Cesium.Math.toRadians(-90),
          roll: 0
        }
      });

      // Initialize styles once Cesium is available in the viewer context
      this.defaultPointStyle = {
        pixelSize: 8,
        color: Cesium.Color.DEEPSKYBLUE,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 2
      };
      this.highlightedPointStyle = {
        pixelSize: 12,
        color: Cesium.Color.AQUA,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 3
      };

      this.fetchAndDisplayVideos();
      this.setupEntityInteraction();
    }
  }

  fetchAndDisplayVideos(): void {
    console.log('Fetching videos...');
    this.videoApiService.getVideos(0, 50).subscribe({
      next: (videoPage: Page<Video>) => {
        console.log('Videos fetched:', videoPage.content.length);
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
        console.log('Adding marker for:', video.title);
        const entity = this.viewer.entities.add({
          id: video.id.toString(), // Ensure ID is a string
          name: video.title,
          position: Cesium.Cartesian3.fromDegrees(video.longitude, video.latitude),
          point: this.defaultPointStyle,
          description: `Video: ${video.title}`
        });
        // Explicitly attach the video object to the entity
        (entity as any).video = video;
      }
    });
  }

  drawTripLines(videos: Video[]): void {
    if (!this.viewer) return;
    
    // Group videos by tripId and sort by tripOrder
    const trips = new Map<string, Video[]>();
    videos.forEach(v => {
      if (v.tripId) {
        if (!trips.has(v.tripId)) trips.set(v.tripId, []);
        trips.get(v.tripId)?.push(v);
      }
    });

    trips.forEach((tripVideos, tripId) => {
      // Sort by tripOrder
      const sorted = tripVideos.sort((a, b) => (a.tripOrder || 0) - (b.tripOrder || 0));
      if (sorted.length < 2) return;

      const positions = sorted
        .filter(v => v.latitude !== undefined && v.longitude !== undefined)
        .map(v => Cesium.Cartesian3.fromDegrees(v.longitude, v.latitude));

      if (positions.length >= 2) {
        this.viewer.entities.add({
          name: `Trip: ${tripId}`,
          polyline: {
            positions: positions,
            width: 3,
            material: new Cesium.PolylineDashMaterialProperty({
              color: Cesium.Color.YELLOW.withAlpha(0.6),
              dashLength: 16
            })
          }
        });
      }
    });
  }

  setupEntityInteraction(): void {
    if (!this.viewer) return;

    this.handler = new Cesium.ScreenSpaceEventHandler(this.viewer.scene.canvas);

    this.handler.setInputAction((click: any) => {
      const pickedObject = this.viewer.scene.pick(click.position);
      
      if (Cesium.defined(pickedObject) && pickedObject.id && (pickedObject.id as any).video) {
        const entity = pickedObject.id;
        const video = (entity as any).video;
        console.log('Entity clicked. Video data:', video);

        this.ngZone.run(() => {
          // Update selected video and show panel
          this.selectedVideo = video;
          this.isInfoPanelVisible = true;
          this.cdr.detectChanges(); // FORZAMOS EL REFRESCO EN MODO ZONELESS

          // Reset previous highlight
          if (this.lastHighlighted && this.lastHighlighted !== entity) {
            this.lastHighlighted.point = this.defaultPointStyle;
          }

          // Highlight current point
          entity.point = this.highlightedPointStyle;
          this.lastHighlighted = entity;
        });

        // Zoom can stay outside zone
        this.viewer.camera.flyTo({
          destination: Cesium.Cartesian3.fromDegrees(video.longitude, video.latitude, 5000),
          duration: 1.5
        });

      }
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK);
  }

  closePanel(): void {
    this.isInfoPanelVisible = false;
    this.selectedVideo = null;
    this.cdr.detectChanges(); // FORZAMOS EL REFRESCO EN MODO ZONELESS
    if (this.lastHighlighted) {
      this.lastHighlighted.point = this.defaultPointStyle;
      this.lastHighlighted = null;
    }
  }
}
