import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuezapCard } from './quezap-card'

describe('ThemeCard', () => {
  let component: QuezapCard
  let fixture: ComponentFixture<QuezapCard>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuezapCard] })
      .compileComponents()

    fixture = TestBed.createComponent(QuezapCard)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
