import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GlobeView } from './globe-view';

describe('GlobeView', () => {
  let component: GlobeView;
  let fixture: ComponentFixture<GlobeView>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GlobeView],
    }).compileComponents();

    fixture = TestBed.createComponent(GlobeView);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
