import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

// These interfaces should match the backend DTOs
export interface Video {
  id: string;
  youtubeId: string;
  title: string;
  description: string;
  latitude: number;
  longitude: number;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class VideoApiService {

  private apiUrl = '/api/videos';

  constructor(private http: HttpClient) { }

  /**
   * Fetches a paginated list of videos from the backend.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @returns An Observable of a Page of Videos.
   */
  getVideos(page: number, size: number): Observable<Page<Video>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<Video>>(this.apiUrl, { params });
  }
}
