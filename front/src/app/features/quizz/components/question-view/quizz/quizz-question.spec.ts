import { ComponentFixture, TestBed } from '@angular/core/testing'

import { patchState } from '@ngrx/signals'
import { of, Subject } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { Tried } from '@quezap/core/types'
import { PictureUrl, QuestionId, QuestionType, QuestionWithAnswers, QuizzQuestion } from '@quezap/domain/models'
import { SESSION_API_SERVICE, SessionApiService } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { QuizzQuestionView } from './quizz-question'

describe('QuizzQuestionView', () => {
  let component: QuizzQuestionView
  let fixture: ComponentFixture<QuizzQuestionView>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let mockSessionApi: Partial<SessionApiService>

  const question2Choices: QuizzQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000010' as QuestionId,
    type: QuestionType.Quizz,
    value: 'Question à 2 choix ?',
    picture: '/quezap-public-images/question.png' as PictureUrl,
    answers: [
      { index: 0, points: 1000, value: 'Choix A' },
      { index: 1, points: 0, value: 'Choix B' },
    ],
  }

  const question4Choices: QuizzQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000011' as QuestionId,
    type: QuestionType.Quizz,
    value: 'Question à 4 choix ?',
    answers: [
      { index: 0, points: 1000, value: 'A' },
      { index: 1, points: 0, value: 'B' },
      { index: 2, points: 0, value: 'C' },
      { index: 3, points: 0, value: 'D' },
    ],
  }

  beforeEach(async () => {
    mockSessionApi = { submitAnswer: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })) }

    await TestBed.configureTestingModule({
      imports: [QuizzQuestionView],
      providers: [
        ActiveSessionStore,
        TimerStore,
        { provide: SESSION_API_SERVICE, useValue: mockSessionApi },
      ],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    fixture = TestBed.createComponent(QuizzQuestionView)
    fixture.componentRef.setInput('question', question2Choices)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create and accept 2..4 answers in responseForm', () => {
    expect(component).toBeTruthy()
    component['selectAnswer'](0)
    fixture.detectChanges()
    expect(component['responseForm']().valid()).toBe(true)

    fixture.componentRef.setInput('question', question4Choices)
    fixture.detectChanges()
    component['selectAnswer'](3)
    fixture.detectChanges()
    expect(component['responseForm']().valid()).toBe(true)
  })

  it('should render #question-image-desc with sr-only class', () => {
    const descEl = fixture.nativeElement.querySelector('#question-image-desc') as HTMLElement | null
    expect(descEl).not.toBeNull()
    expect(descEl?.classList.contains('sr-only')).toBe(true)
  })

  it('should lock controls and ignore votes when session is paused', () => {
    patchState(sessionStore, { isSessionPaused: true })
    fixture.detectChanges()

    const fieldset = fixture.nativeElement.querySelector('fieldset') as HTMLFieldSetElement
    expect(fieldset.disabled).toBe(true)
    expect(fieldset.inert).toBe(true)

    component['selectAnswer'](0)
    expect(mockSessionApi.submitAnswer).not.toHaveBeenCalled()
    expect(component['hasSubmitted']()).toBe(false)
  })

  it('should rollback optimistic choice and display clear error message when submitAnswer fails', () => {
    const pending$ = new Subject<Tried<void>>()
    mockSessionApi.submitAnswer = jest.fn().mockReturnValue(pending$.asObservable())

    component['selectAnswer'](1)
    fixture.detectChanges()

    expect(component['isSubmitting']()).toBe(true)
    expect(component['hasSubmitted']()).toBe(false)
    expect(sessionStore.mySubmittedChoice()).toBeUndefined()

    pending$.next({
      kind: 'failure',
      error: new ValidationError({}, 'Réponse tardive : les réponses sont déjà verrouillées pour cette question.'),
    })
    fixture.detectChanges()

    expect(component['isSubmitting']()).toBe(false)
    expect(component['hasSubmitted']()).toBe(false)
    expect(component['responses']()).toEqual({ 0: false, 1: false })
    expect(sessionStore.mySubmittedChoice()).toBeUndefined()

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null
    expect(alert?.textContent).toContain('verrouillées')
    expect(fixture.nativeElement.textContent).not.toContain('Choix enregistré !')
  })

  it('should unsubscribe in-flight submission when component is destroyed', () => {
    const pending$ = new Subject<Tried<void>>()
    mockSessionApi.submitAnswer = jest.fn().mockReturnValue(pending$.asObservable())

    component['selectAnswer'](0)
    expect(pending$.observed).toBe(true)

    fixture.destroy()
    expect(pending$.observed).toBe(false)
  })
})
