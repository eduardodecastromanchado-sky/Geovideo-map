import { Component, signal, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
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

  ngAfterViewInit(): void {
    if (this.introVideo) {
      const video = this.introVideo.nativeElement;
      video.muted = true; // Forzar silencio para asegurar autoplay
      
      // Aumentamos a 1000ms (1 segundo) para evitar conflictos de carga inicial
      setTimeout(() => {
        video.play()
          .then(() => {
            console.log('Autoplay iniciado con éxito tras 1s');
            this.showPlayHint = false;
          })
          .catch(err => {
            console.warn('Autoplay bloqueado. Requiere interacción.', err);
            this.showPlayHint = true;
          });
      }, 1000);

      // Verificación adicional al segundo 1
      setTimeout(() => {
        if (video.paused && this.showSplash) {
          this.showPlayHint = true;
        }
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
  }

  onTimeUpdate(event: Event): void {
    const video = event.target as HTMLVideoElement;
    // Activamos la transición 1.2 segundos antes del final para que coincida con el logo
    if (video.duration && video.duration - video.currentTime < 1.2) {
      if (!this.isFadingOut) {
        console.log('Iniciando transición cinematográfica final.');
        this.isFadingOut = true;
      }
    }
  }
}
