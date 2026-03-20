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
  @ViewChild('introVideo') introVideo!: ElementRef<HTMLVideoElement>;
  protected readonly title = signal('geovideo-frontend');
  showSplash = true;
  isFadingOut = false;
  showSubscribeModal = false;

  ngAfterViewInit(): void {
    // Iniciamos el temporizador para la intro (duración ~6s)
    setTimeout(() => {
      this.isFadingOut = true;
      setTimeout(() => this.onSplashEnded(), 2000); // 2s de fade-out
    }, 4500); 
  }

  playManual(): void {
    // No longer used for CSS intro, but kept for compatibility
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
    console.log('Splash screen ended.');
    this.showSplash = false;
  }

  // onTimeUpdate is no longer needed but kept to avoid errors if triggered
  onTimeUpdate(event: Event): void {}
}
