import { ComponentFixture, TestBed } from '@angular/core/testing'

import { patchState } from '@ngrx/signals'
import { of, Subject } from 'rxjs'

import { ForbidenError } from '@quezap/core/errors'
import { Tried } from '@quezap/core/types'
import { BinaryQuestion, PictureUrl, QuestionId, QuestionType, QuestionWithAnswers } from '@quezap/domain/models'
import { SESSION_API_SERVICE, SessionApiService } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { BinaryQuestionView } from './binary-question'

describe('BinaryQuestionView', () => {
  let component: BinaryQuestionView
  let fixture: ComponentFixture<BinaryQuestionView>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let mockSessionApi: Partial<SessionApiService>

  const question1: BinaryQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000020' as QuestionId,
    type: QuestionType.Binary,
    value: 'Plutôt thé ou café ?',
    picture: '/quezap-public-images/binary.png' as PictureUrl,
    answers: [
      { index: 0, points: 1000, value: 'Thé' },
      { index: 1, points: 1000, value: 'Café' },
    ],
  }

  beforeEach(async () => {
    mockSessionApi = { submitAnswer: jest.fn().mockReturnValue(of({ kind: 'success', result: undefined })) }

    await TestBed.configureTestingModule({
      imports: [BinaryQuestionView],
      providers: [
        ActiveSessionStore,
        TimerStore,
        { provide: SESSION_API_SERVICE, useValue: mockSessionApi },
      ],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    fixture = TestBed.createComponent(BinaryQuestionView)
    fixture.componentRef.setInput('question', question1)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
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

  it('should rollback selection and display alert on 403 error', () => {
    const pending$ = new Subject<Tried<void>>()
    mockSessionApi.submitAnswer = jest.fn().mockReturnValue(pending$.asObservable())

    component['selectAnswer'](0)
    pending$.next({
      kind: 'failure',
      error: new ForbidenError('Tu as été exclu de cette session.'),
    })
    fixture.detectChanges()

    expect(component['hasSubmitted']()).toBe(false)
    expect(component['responses']()).toEqual({ 0: false, 1: false })
    expect(sessionStore.mySubmittedChoice()).toBeUndefined()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('exclu')
  })
})
