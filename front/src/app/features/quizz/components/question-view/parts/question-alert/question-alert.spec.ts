import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionAlert } from './question-alert'

describe('QuestionAlert', () => {
  let component: QuestionAlert
  let fixture: ComponentFixture<QuestionAlert>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionAlert] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionAlert)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should display timer warning when timeLeft <= 15 and hide when isRevealing', () => {
    fixture.componentRef.setInput('timeLeft', 10)
    fixture.detectChanges()
    expect(component['showTimerWarning']()).toBe(true)

    fixture.componentRef.setInput('isRevealing', true)
    fixture.detectChanges()
    expect(component['showTimerWarning']()).toBe(false)
  })

  it('should hide timesUp alert when isRevealing is true', () => {
    fixture.componentRef.setInput('timeLeft', 0)
    fixture.detectChanges()
    expect(component['timesUp']()).toBe(true)

    fixture.componentRef.setInput('isRevealing', true)
    fixture.detectChanges()
    expect(component['timesUp']()).toBe(false)
  })
})
