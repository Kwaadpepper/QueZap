import { ComponentFixture, TestBed } from '@angular/core/testing'

import { TypeSelector } from './type-selector'

describe('TypeSelector', () => {
  let component: TypeSelector
  let fixture: ComponentFixture<TypeSelector>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TypeSelector] })
      .compileComponents()

    fixture = TestBed.createComponent(TypeSelector)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
