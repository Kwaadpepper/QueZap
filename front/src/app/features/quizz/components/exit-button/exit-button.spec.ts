import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ExitButton } from './exit-button'

describe('ExitButton', () => {
  let component: ExitButton
  let fixture: ComponentFixture<ExitButton>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ExitButton] })
      .compileComponents()

    fixture = TestBed.createComponent(ExitButton)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
