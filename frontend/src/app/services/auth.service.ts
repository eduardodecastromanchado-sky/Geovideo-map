import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface UserDto {
  id: number;
  email: string;
  displayName: string;
  provider: string;
  avatarUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  /** Current logged-in user. null = not loaded yet, undefined = anonymous. */
  readonly currentUser = signal<UserDto | null | undefined>(null);

  private readonly base = '/api/auth';

  constructor(private http: HttpClient) {}

  /** Called once at app startup to restore session from cookie. */
  async init(): Promise<void> {
    try {
      const user = await firstValueFrom(
        this.http.get<UserDto>(`${this.base}/me`, { withCredentials: true })
      );
      this.currentUser.set(user);
    } catch {
      this.currentUser.set(undefined);
    }
  }

  async register(email: string, password: string, displayName?: string): Promise<UserDto> {
    const user = await firstValueFrom(
      this.http.post<UserDto>(`${this.base}/register`, { email, password, displayName },
        { withCredentials: true })
    );
    this.currentUser.set(user);
    return user;
  }

  async login(email: string, password: string, rememberMe: boolean): Promise<UserDto> {
    const user = await firstValueFrom(
      this.http.post<UserDto>(`${this.base}/login`, { email, password, rememberMe },
        { withCredentials: true })
    );
    this.currentUser.set(user);
    return user;
  }

  async logout(): Promise<void> {
    await firstValueFrom(
      this.http.post<void>(`${this.base}/logout`, {}, { withCredentials: true })
    );
    this.currentUser.set(undefined);
  }

  async updateProfile(displayName: string, password?: string): Promise<UserDto> {
    const body: any = { displayName };
    if (password) body.password = password;
    const user = await firstValueFrom(
      this.http.put<UserDto>(`${this.base}/me`, body, { withCredentials: true })
    );
    this.currentUser.set(user);
    return user;
  }

  /** Returns the display label: displayName, or local-part of email, or '…'. */
  greetingName(user: UserDto): string {
    if (user.displayName?.trim()) return user.displayName.trim();
    return user.email.split('@')[0];
  }
}
