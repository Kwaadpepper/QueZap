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
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
