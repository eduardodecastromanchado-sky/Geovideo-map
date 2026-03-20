import { Component, signal, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GlobeViewComponent } from './components/globe-view/globe-view';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobeViewComponent, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements AfterViewInit {
  @ViewChild('introRef') introVideo!: ElementRef<HTMLVideoElement>;
  protected readonly title = signal('geovideo-frontend');
  showSplash = true;
  isFadingOut = false;
  showSubscribeModal = false;
  showPlayHint = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    if (this.introVideo) {
      const video = this.introVideo.nativeElement;
      video.muted = true; // Forzar silencio para asegurar autoplay
      
      // Aumentamos a 1000ms (1 segundo) para evitar conflictos de carga inicial
      setTimeout(() => {
        video.playbackRate = 1.5; // ACELERACIÓN A 1.5X POR PETICIÓN DEL USUARIO
        video.play()
          .then(() => {
            console.log('Autoplay iniciado con éxito tras 1s a 1.5x');
            this.showPlayHint = false;
            this.cdr.detectChanges();
          })
          .catch(err => {
            console.warn('Autoplay bloqueado. Requiere interacción.', err);
            this.showPlayHint = true;
            this.cdr.detectChanges();
          });
      }, 1000);

      // SEGURIDAD: Si después de 10 segundos la intro no ha terminado, la quitamos a la fuerza
      setTimeout(() => {
        if (this.showSplash) {
          console.log('Safety Timeout: Forzando cierre de splash screen.');
          this.onSplashEnded();
        }
      }, 10000);
    }
  }

  playManual(): void {
    if (this.introVideo && this.introVideo.nativeElement.paused) {
      this.introVideo.nativeElement.play();
      this.showPlayHint = false;
      this.cdr.detectChanges();
    }
  }

  openSubscribeModal(): void {
    this.showSubscribeModal = true;
  }

  closeSubscribeModal(): void {
    this.showSubscribeModal = false;
  }

  goToYoutube(): void {
    window.open('https://www.youtube.com/@SkyDrift-c5n?sub_confirmation=1', '_blank');
  }

  onSplashEnded(): void {
    this.showSplash = false;
    this.cdr.detectChanges();
  }

  onTimeUpdate(event: Event): void {
    const video = event.target as HTMLVideoElement;
    // Activamos la transición 1.2 segundos antes del final
    if (video.duration && video.duration - video.currentTime < 1.2) {
      if (!this.isFadingOut) {
        console.log('Iniciando transición cinematográfica final.');
        this.isFadingOut = true;
        this.cdr.detectChanges();
      }
    }
  }

  onVideoEnded(): void {
    this.onSplashEnded();
  }
}
