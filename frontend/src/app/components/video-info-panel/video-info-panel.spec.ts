import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VideoInfoPanel } from './video-info-panel';

describe('VideoInfoPanel', () => {
  let component: VideoInfoPanel;
  let fixture: ComponentFixture<VideoInfoPanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VideoInfoPanel],
    }).compileComponents();

    fixture = TestBed.createComponent(VideoInfoPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
