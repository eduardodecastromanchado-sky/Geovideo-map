import { Component, Input, Output, EventEmitter, HostBinding } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Video } from '../../services/video-api';

@Component({
  selector: 'app-video-info-panel',
  templateUrl: './video-info-panel.html',
  styleUrls: ['./video-info-panel.scss']
})
export class VideoInfoPanelComponent {
  
  @Input() video: Video | null = null;
  @Output() close = new EventEmitter<void>();

  @HostBinding('class.hidden')
  get isHidden() {
    return this.video === null;
  }

  constructor(private sanitizer: DomSanitizer) { }

  getSafeVideoUrl(): SafeResourceUrl | null {
    if (this.video) {
      const url = `https://www.youtube.com/embed/${this.video.youtubeId}`;
      return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }
    return null;
  }
}
