import { Component, Input, Output, EventEmitter, HostBinding, OnChanges, SimpleChanges } from '@angular/core';
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

  @HostBinding('class.hidden')
  get isHidden() {
    return this.video === null;
  }

  constructor(private sanitizer: DomSanitizer) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['video'] && this.video) {
      this.updateSafeVideoUrl();
    }
  }

  private updateSafeVideoUrl(): void {
    if (this.video && this.video.youtubeId) {
      console.log('Updating safe video URL for:', this.video.youtubeId);
      const url = `https://www.youtube.com/embed/${this.video.youtubeId}`;
      this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    } else {
      console.warn('No video or youtubeId provided to info panel');
      this.safeVideoUrl = null;
    }
  }
}
