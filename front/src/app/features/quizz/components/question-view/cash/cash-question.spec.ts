import { ComponentFixture, TestBed } from '@angular/core/testing'

import { patchState } from '@ngrx/signals'
import { of, Subject } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { Tried } from '@quezap/core/types'
import { CashQuestion, PictureUrl, QuestionId, QuestionType, QuestionWithAnswers } from '@quezap/domain/models'
import { SESSION_API_SERVICE } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { CashQuestionView } from './cash-question'

describe('CashQuestionView', () => {
  let component: CashQuestionView
  let fixture: ComponentFixture<CashQuestionView>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let mockSessionApi: { submitAnswer: jest.Mock }

  const question1: CashQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000001' as QuestionId,
    type: QuestionType.Cash,
    value: 'Capitale des Pays-Bas ?',
    picture: '/quezap-public-images/cash.png' as PictureUrl,
    answers: [{ index: 0, points: 1000, value: 'Amsterdam' }],
  }

  const question2: CashQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000002' as QuestionId,
    type: QuestionType.Cash,
    value: 'Fleur emblème ?',
    answers: [{ index: 0, points: 1000, value: 'Tulipe' }],
  }

  beforeEach(async () => {
    mockSessionApi = { submitAnswer: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })) }

    await TestBed.configureTestingModule({
      imports: [CashQuestionView],
      providers: [
        ActiveSessionStore,
        TimerStore,
        { provide: SESSION_API_SERVICE, useValue: mockSessionApi },
      ],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    fixture = TestBed.createComponent(CashQuestionView)
    fixture.componentRef.setInput('question', question1)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should submit text answer and update status', () => {
    component['textInput'].set(' Amsterdam ')
    component['submitAnswer']()
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(true)
    expect(component['lastSubmittedText']()).toBe('Amsterdam')
    expect(mockSessionApi.submitAnswer).toHaveBeenCalledWith(0, 'Amsterdam')
  })

  it('should not submit blank text', () => {
    component['textInput'].set('   ')
    component['submitAnswer']()

    expect(component['hasSubmitted']()).toBe(false)
    expect(mockSessionApi.submitAnswer).not.toHaveBeenCalled()
  })

  it('should reset when switching questions', () => {
    component['textInput'].set('Amsterdam')
    component['submitAnswer']()
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(true)

    // Re-emitting same question ID (e.g. SSE reconnect) preserves submitted state
    fixture.componentRef.setInput('question', { ...question1 })
    fixture.detectChanges()
    expect(component['hasSubmitted']()).toBe(true)
    expect(component['lastSubmittedText']()).toBe('Amsterdam')

    // Switch question to new ID resets state
    fixture.componentRef.setInput('question', question2)
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(false)
    expect(component['textInput']()).toBe('')
    expect(component['lastSubmittedText']()).toBe('')
  })

  it('should render #question-image-desc with sr-only class', () => {
    const descEl = fixture.nativeElement.querySelector('#question-image-desc') as HTMLElement | null
    expect(descEl).not.toBeNull()
    expect(descEl?.classList.contains('sr-only')).toBe(true)
  })

  it('should disable input and block submission when session is paused', () => {
    patchState(sessionStore, { isSessionPaused: true })
    component['textInput'].set('Amsterdam')
    fixture.detectChanges()

    const inputEl = fixture.nativeElement.querySelector('#cash-input') as HTMLInputElement
    const submitBtn = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement
    expect(inputEl.disabled).toBe(true)
    expect(submitBtn.disabled).toBe(true)

    component['submitAnswer']()
    expect(mockSessionApi.submitAnswer).not.toHaveBeenCalled()
  })

  it('should rollback submission confirmation and display error on failure', () => {
    const pending$ = new Subject<Tried<void>>()
    mockSessionApi.submitAnswer = jest.fn().mockReturnValue(pending$.asObservable())

    component['textInput'].set('Amsterdam')
    component['submitAnswer']()
    pending$.next({
      kind: 'failure',
      error: new ServiceError('Connexion perdue : impossible d\'envoyer ta réponse.'),
    })
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(false)
    expect(component['lastSubmittedText']()).toBe('')
    expect(sessionStore.mySubmittedChoice()).toBeUndefined()
    expect(component['textInput']()).toBe('Amsterdam')
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Connexion perdue')
  })
})
