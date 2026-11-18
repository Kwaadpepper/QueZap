import { ComponentFixture, TestBed } from '@angular/core/testing'

import { patchState } from '@ngrx/signals'
import { of, Subject } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { Tried } from '@quezap/core/types'
import { BooleanQuestion, PictureUrl, QuestionId, QuestionType } from '@quezap/domain/models'
import { SESSION_API_SERVICE, SessionApiService } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { BooleanQuestionView } from './boolean-question'

describe('BooleanQuestionView', () => {
  let component: BooleanQuestionView
  let fixture: ComponentFixture<BooleanQuestionView>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let mockSessionApi: Partial<SessionApiService>

  const question1: BooleanQuestion = {
    id: '018e3a2b-8888-7def-8123-000000000001' as QuestionId,
    type: QuestionType.Boolean,
    value: 'Première affirmation',
    picture: '/quezap-public-images/bool.png' as PictureUrl,
  }

  const question2: BooleanQuestion = {
    id: '018e3a2b-8888-7def-8123-000000000002' as QuestionId,
    type: QuestionType.Boolean,
    value: 'Deuxième affirmation',
  }

  beforeEach(async () => {
    mockSessionApi = { submitAnswer: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })) }

    await TestBed.configureTestingModule({
      imports: [BooleanQuestionView],
      providers: [
        ActiveSessionStore,
        TimerStore,
        { provide: SESSION_API_SERVICE, useValue: mockSessionApi },
      ],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    fixture = TestBed.createComponent(BooleanQuestionView)
    fixture.componentRef.setInput('question', question1)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should reset responses and hasSubmitted when switching between two consecutive boolean questions', () => {
    component['selectAnswer'](0)
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(true)
    expect(component['responses']()).toEqual({ 0: true, 1: false })

    fixture.componentRef.setInput('question', question2)
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(false)
    expect(component['responses']()).toEqual({ 0: false, 1: false })

    component['selectAnswer'](1)
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(true)
    expect(component['responses']()).toEqual({ 0: false, 1: true })
  })

  it('should render #question-image-desc with sr-only class', () => {
    const descEl = fixture.nativeElement.querySelector('#question-image-desc') as HTMLElement | null
    expect(descEl).not.toBeNull()
    expect(descEl?.classList.contains('sr-only')).toBe(true)
  })

  it('should lock controls and block vote when session is paused', () => {
    patchState(sessionStore, { isSessionPaused: true })
    fixture.detectChanges()

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement
    expect(form.inert).toBe(true)

    component['selectAnswer'](0)
    expect(mockSessionApi.submitAnswer).not.toHaveBeenCalled()
    expect(component['hasSubmitted']()).toBe(false)
  })

  it('should rollback selection and show error when submission fails', () => {
    const pending$ = new Subject<Tried<void>>()
    mockSessionApi.submitAnswer = jest.fn().mockReturnValue(pending$.asObservable())

    component['selectAnswer'](0)
    pending$.next({
      kind: 'failure',
      error: new ValidationError({}, 'Temps écoulé : ta réponse n\'a pas été prise en compte.'),
    })
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(false)
    expect(component['responses']()).toEqual({ 0: false, 1: false })
    expect(sessionStore.mySubmittedChoice()).toBeUndefined()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Temps écoulé')
  })
})
