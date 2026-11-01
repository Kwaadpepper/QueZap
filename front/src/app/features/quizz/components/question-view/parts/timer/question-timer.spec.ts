import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing'

import { TimerStore } from '@quezap/features/quizz/stores'

import { QuestionTimer } from './question-timer'

describe('QuestionTimer', () => {
  let component: QuestionTimer
  let fixture: ComponentFixture<QuestionTimer>

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [QuestionTimer] })
      .compileComponents()

    fixture = TestBed.createComponent(QuestionTimer)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should synchronize remainingSeconds using timer deadline', fakeAsync(() => {
    const store = TestBed.inject(TimerStore)
    const futureDate = new Date(Date.now() + 15000).toISOString()
    store.setTimer({ seconds: 20, deadline: futureDate })
    tick(10)

    fixture.detectChanges()
    expect(component['limit']()).toBe(20)
  }))

  it('should freeze countdown when session is paused', fakeAsync(() => {
    const timerStore = TestBed.inject(TimerStore)
    timerStore.setTimer({ seconds: 10 })
    tick(10)
    fixture.detectChanges()

    expect(component['remainingSeconds']()).toBe(10)
  }))
})
