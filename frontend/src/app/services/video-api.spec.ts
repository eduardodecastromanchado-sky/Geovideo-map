import { TestBed } from '@angular/core/testing';

import { VideoApi } from './video-api';

describe('VideoApi', () => {
  let service: VideoApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(VideoApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
