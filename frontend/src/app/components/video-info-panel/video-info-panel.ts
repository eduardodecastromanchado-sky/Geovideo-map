import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, HostBinding, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Video } from '../../services/video-api';

@Component({
  selector: 'app-video-info-panel',
  templateUrl: './video-info-panel.html',
  styleUrls: ['./video-info-panel.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class VideoInfoPanelComponent implements OnChanges {
  
  @Input() video: Video | null = null;
  @Output() close = new EventEmitter<void>();

  safeVideoUrl: SafeResourceUrl | null = null;
  isVideoVisible: boolean = false;

  @HostBinding('class.hidden')
  get isHidden() {
    return this.video === null;
  }

  playVideo(): void {
    this.isVideoVisible = true;
  }

  constructor(private sanitizer: DomSanitizer, private cdr: ChangeDetectorRef) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['video'] && this.video) {
      this.isVideoVisible = false;
      this.updateSafeVideoUrl();
    }
  }

  private updateSafeVideoUrl(): void {
    if (this.video && this.video.youtubeId) {
      console.log('Updating safe video URL for:', this.video.youtubeId);
      // Añadimos autoplay=1 y mute=1 para que el vídeo arranque solo al abrir el panel
      const url = `https://www.youtube.com/embed/${this.video.youtubeId}?autoplay=1&mute=1`;
      this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
      
      this.cdr.detectChanges(); // Aseguramos el refresco en modo Zoneless
    } else {
      console.warn('No video or youtubeId provided to info panel');
      this.safeVideoUrl = null;
    }
  }
}
