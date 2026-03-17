import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';

declare const Cesium: any; // Use 'any' to avoid TypeScript errors for the global Cesium object

@Component({
  selector: 'app-globe-view',
  templateUrl: './globe-view.html',
  styleUrls: ['./globe-view.scss']
})
export class GlobeViewComponent implements AfterViewInit {

  @ViewChild('cesiumContainer') cesiumContainer!: ElementRef;

  constructor() { }

  ngAfterViewInit(): void {
    if (this.cesiumContainer) {
      const viewer = new Cesium.Viewer(this.cesiumContainer.nativeElement, {
        // Viewer options to keep it simple initially
        animation: false,
        timeline: false,
        homeButton: false,
        geocoder: false,
        navigationHelpButton: false,
        baseLayerPicker: false,
        sceneModePicker: false
      });
      
      // TODO: Add video data as entities/points on the globe
    }
  }
}
