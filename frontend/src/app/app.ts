import {
  Component, signal, ViewChild, ElementRef, AfterViewInit,
  ChangeDetectorRef, OnInit, HostListener, effect
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GlobeViewComponent } from './components/globe-view/globe-view';
import { AuthService, UserDto } from './services/auth.service';

type AuthTab = 'login' | 'register';
type UiPanel = 'none' | 'auth' | 'profile-menu' | 'profile-edit' | 'menu-drawer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobeViewComponent, CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements AfterViewInit, OnInit {

  // ── splash / subscribe (unchanged) ──────────────────────────────────────────
  @ViewChild('introRef') introVideo!: ElementRef<HTMLVideoElement>;
  protected readonly title = signal('geovideo-frontend');
  showSplash = true;
  isFadingOut = false;
  showSubscribeModal = false;
  showPlayHint = false;

  // ── auth state ───────────────────────────────────────────────────────────────
  activePanel: UiPanel = 'none';
  authTab: AuthTab = 'login';

  // login / register form fields
  authEmail = '';
  authPassword = '';
  authDisplayName = '';
  authRememberMe = false;
  authError = '';
  authLoading = false;

  // profile edit form
  profileDisplayName = '';
  profilePassword = '';
  profileError = '';
  profileLoading = false;

  constructor(
    private cdr: ChangeDetectorRef,
    public authService: AuthService
  ) {
    // React when currentUser changes (e.g. after Google OAuth redirect)
    effect(() => {
      const u = this.authService.currentUser();
      if (u && this.activePanel === 'auth') this.closePanel();
    });
  }

  async ngOnInit(): Promise<void> {
    await this.authService.init();
    this.cdr.detectChanges();
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  get currentUser(): UserDto | null | undefined {
    return this.authService.currentUser();
  }

  get greetingName(): string {
    const u = this.currentUser;
    return u ? this.authService.greetingName(u) : '';
  }

  // ── panel control ─────────────────────────────────────────────────────────────

  openAuthModal(): void {
    this.authTab = 'login';
    this.resetAuthForm();
    this.activePanel = 'auth';
  }

  openProfileMenu(): void {
    this.activePanel = this.activePanel === 'profile-menu' ? 'none' : 'profile-menu';
  }

  openProfileEdit(): void {
    const u = this.currentUser;
    this.profileDisplayName = u?.displayName ?? '';
    this.profilePassword = '';
    this.profileError = '';
    this.activePanel = 'profile-edit';
  }

  openMenuDrawer(): void {
    this.activePanel = this.activePanel === 'menu-drawer' ? 'none' : 'menu-drawer';
  }

  closePanel(): void {
    this.activePanel = 'none';
    this.authError = '';
    this.profileError = '';
  }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.closePanel(); }

  // ── auth actions ──────────────────────────────────────────────────────────────

  async doLogin(): Promise<void> {
    this.authLoading = true;
    this.authError = '';
    try {
      await this.authService.login(this.authEmail, this.authPassword, this.authRememberMe);
      this.closePanel();
    } catch (e: any) {
      this.authError = e?.error ?? 'Error al iniciar sesión';
    } finally {
      this.authLoading = false;
      this.cdr.detectChanges();
    }
  }

  async doRegister(): Promise<void> {
    this.authLoading = true;
    this.authError = '';
    try {
      await this.authService.register(this.authEmail, this.authPassword, this.authDisplayName || undefined);
      this.closePanel();
    } catch (e: any) {
      this.authError = e?.error ?? 'Error al registrarse';
    } finally {
      this.authLoading = false;
      this.cdr.detectChanges();
    }
  }

  goToGoogle(): void {
    // Redirects to Spring backend OAuth2 flow — no Angular routing involved
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }

  async doLogout(): Promise<void> {
    await this.authService.logout();
    this.closePanel();
    this.cdr.detectChanges();
  }

  async doSaveProfile(): Promise<void> {
    this.profileLoading = true;
    this.profileError = '';
    try {
      await this.authService.updateProfile(
        this.profileDisplayName,
        this.profilePassword || undefined
      );
      this.closePanel();
    } catch (e: any) {
      this.profileError = e?.error ?? 'Error al guardar perfil';
    } finally {
      this.profileLoading = false;
      this.cdr.detectChanges();
    }
  }

  private resetAuthForm(): void {
    this.authEmail = '';
    this.authPassword = '';
    this.authDisplayName = '';
    this.authRememberMe = false;
    this.authError = '';
  }

  // ── splash (unchanged) ────────────────────────────────────────────────────────

  ngAfterViewInit(): void {
    if (this.introVideo) {
      const video = this.introVideo.nativeElement;
      video.muted = true;
      setTimeout(() => {
        video.playbackRate = 1.5;
        video.play()
          .then(() => {
            this.showPlayHint = false;
            this.cdr.detectChanges();
          })
          .catch(() => {
            this.showPlayHint = true;
            this.cdr.detectChanges();
          });
      }, 1000);

      setTimeout(() => {
        if (this.showSplash) this.onSplashEnded();
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

  openSubscribeModal(): void { this.showSubscribeModal = true; }
  closeSubscribeModal(): void { this.showSubscribeModal = false; }
  goToYoutube(): void {
    window.open('https://www.youtube.com/@SkyDrift-c5n?sub_confirmation=1', '_blank');
  }

  onSplashEnded(): void {
    this.showSplash = false;
    this.cdr.detectChanges();
  }

  onTimeUpdate(event: Event): void {
    const video = event.target as HTMLVideoElement;
    if (video.duration && video.duration - video.currentTime < 1.2) {
      if (!this.isFadingOut) {
        this.isFadingOut = true;
        this.cdr.detectChanges();
      }
    }
  }

  onVideoEnded(): void { this.onSplashEnded(); }
}
