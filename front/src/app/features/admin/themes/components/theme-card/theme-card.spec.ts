import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ThemeCard } from './theme-card'

describe('ThemeCard', () => {
  let component: ThemeCard
  let fixture: ComponentFixture<ThemeCard>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ThemeCard] })
      .compileComponents()

    fixture = TestBed.createComponent(ThemeCard)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
