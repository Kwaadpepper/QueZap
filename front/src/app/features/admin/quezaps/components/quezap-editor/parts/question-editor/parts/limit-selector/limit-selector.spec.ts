import { ComponentFixture, TestBed } from '@angular/core/testing'

import { LimitSelector } from './limit-selector'

describe('LimitSelector', () => {
  let component: LimitSelector
  let fixture: ComponentFixture<LimitSelector>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LimitSelector] })
      .compileComponents()

    fixture = TestBed.createComponent(LimitSelector)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should not include 0 seconds option and default to 30 seconds', () => {
    expect(component['availableLimitSeconds']().some(o => o.value === 0)).toBe(false)
    expect(component['selectedLimit']()).toBe(30)
  })
})
