import { ComponentFixture, TestBed } from '@angular/core/testing'

import { TimerStore } from '@quezap/features/quizz/stores'

import { QuestionTimer } from './question-timer'

describe('QuestionTimer', () => {
  let component: QuestionTimer
  let fixture: ComponentFixture<QuestionTimer>

  beforeEach(async () => {
    jest.useFakeTimers()
    await TestBed.configureTestingModule({ imports: [QuestionTimer] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionTimer)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should synchronize remainingSeconds using timer deadline', () => {
    const store = TestBed.inject(TimerStore)
    const futureDate = new Date(Date.now() + 15000).toISOString()
    store.setTimer({ seconds: 20, deadline: futureDate })
    jest.advanceTimersByTime(10)

    fixture.detectChanges()
    expect(component['limit']()).toBe(20)
  })

  it('should freeze countdown when session is paused', () => {
    const timerStore = TestBed.inject(TimerStore)
    timerStore.setTimer({ seconds: 10 })
    jest.advanceTimersByTime(10)
    fixture.detectChanges()

    expect(component['remainingSeconds']()).toBe(10)
  })
})
