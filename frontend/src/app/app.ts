import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { GlobeViewComponent } from './components/globe-view/globe-view';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobeViewComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('geovideo-frontend');
}
