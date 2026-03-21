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
      // v4.0 - RESET TOTAL A CONFIGURACIÓN NATIVA ESTABLE
      // Dejamos que Cesium use su proveedor satelital por defecto (Ion)
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
        requestRenderMode: false // Renderizado continuo para evitar pantallas negras/congeladas
      });

      // AJUSTES CRÍTICOS - Basados en CesiumJS Issues #7871, #10442 y #12936
      const scene = this.viewer.scene;
      const globe = scene.globe;
      
      // FIX UNIVERSAL (PC y Móvil)
      scene.highDynamicRange = false; // HDR causa globos negros en GPUs Adreno (Qualcomm)
      globe.enableLighting = false; // Sin iluminación dinámica (evita sombra nocturna)
      globe.showGroundAtmosphere = false; // CLAVE: Este + enableLighting = globo negro en Adreno (#7871)
      scene.logarithmicDepthBuffer = false;
      globe.depthTestAgainstTerrain = false;
      
      // FIX ESPECÍFICO PARA MÓVILES ANDROID (Chrome 140+ / Adreno / Mali)
      const isMobile = /Mobi|Android/i.test(navigator.userAgent);
      if (isMobile) {
        this.viewer.resolutionScale = 0.85; // Calidad definitiva en móvil
        globe.maximumScreenSpaceError = 2; // Máximo detalle de tiles
        scene.skyAtmosphere.show = false;
        scene.fog.enabled = false;
        scene.fxaa = false;
      }

      // Cámara inicial - ACERCAMOS A 4,000km (en lugar de 15,000km)
      // A esta distancia los móviles NO suelen fallar en la precisión de renderizado
      this.viewer.camera.setView({
        destination: Cesium.Cartesian3.fromDegrees(-70, 18, 4000000), 
        orientation: {
          heading: 0,
          pitch: Cesium.Math.toRadians(-90),
          roll: 0
        }
      });

      // Estilos de puntos
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
        const entity = this.viewer.entities.add({
          id: video.id.toString(),
          name: video.title,
          position: Cesium.Cartesian3.fromDegrees(video.longitude, video.latitude),
          point: this.defaultPointStyle,
          description: `Video: ${video.title}`
        });
        (entity as any).video = video;
      }
    });
  }

  drawTripLines(videos: Video[]): void {
    if (!this.viewer) return;
    const trips = new Map<string, Video[]>();
    videos.forEach(v => {
      if (v.tripId) {
        if (!trips.has(v.tripId)) trips.set(v.tripId, []);
        trips.get(v.tripId)?.push(v);
      }
    });

    trips.forEach((tripVideos, tripId) => {
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

        this.ngZone.run(() => {
          this.selectedVideo = video;
          this.isInfoPanelVisible = true;
          this.cdr.detectChanges();

          if (this.lastHighlighted && this.lastHighlighted !== entity) {
            this.lastHighlighted.point = this.defaultPointStyle;
          }

          entity.point = this.highlightedPointStyle;
          this.lastHighlighted = entity;
        });

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
    this.cdr.detectChanges();
    if (this.lastHighlighted) {
      this.lastHighlighted.point = this.defaultPointStyle;
      this.lastHighlighted = null;
    }
  }
}
