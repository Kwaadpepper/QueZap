import { ComponentFixture, TestBed } from '@angular/core/testing'

import { PointsSelector } from './points-selector'

describe('PointsSelector', () => {
  let component: PointsSelector
  let fixture: ComponentFixture<PointsSelector>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PointsSelector] })
      .compileComponents()

    fixture = TestBed.createComponent(PointsSelector)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should update selected points when setQuickPoints is called', () => {
    component['setQuickPoints'](2000)
    expect(component['selectedPoints']()).toBe(2000)
  })
})
