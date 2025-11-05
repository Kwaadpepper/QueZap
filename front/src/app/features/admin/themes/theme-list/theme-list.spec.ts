import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThemeList } from './theme-list';

describe('ThemeList', () => {
  let component: ThemeList;
  let fixture: ComponentFixture<ThemeList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ThemeList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ThemeList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
