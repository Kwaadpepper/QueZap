import { ComponentFixture, TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import { BooleanQuestion, QuestionId, QuestionType } from '@quezap/domain/models'
import { SESSION_API_SERVICE, SessionApiService } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { BooleanQuestionView } from './boolean-question'

describe('BooleanQuestionView', () => {
  let component: BooleanQuestionView
  let fixture: ComponentFixture<BooleanQuestionView>
  let mockSessionApi: Partial<SessionApiService>

  const question1: BooleanQuestion = {
    id: '018e3a2b-8888-7def-8123-000000000001' as QuestionId,
    type: QuestionType.Boolean,
    value: 'Première affirmation',
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
})
