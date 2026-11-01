import { ComponentFixture, TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import { CashQuestion, QuestionId, QuestionType, QuestionWithAnswers } from '@quezap/domain/models'
import { SESSION_API_SERVICE } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { CashQuestionView } from './cash-question'

describe('CashQuestionView', () => {
  let component: CashQuestionView
  let fixture: ComponentFixture<CashQuestionView>
  let mockSessionApi: { submitAnswer: jest.Mock }

  const question1: CashQuestion & QuestionWithAnswers = {
    id: '018e3a2b-8888-7def-8123-000000000001' as QuestionId,
    type: QuestionType.Cash,
    value: 'Capitale des Pays-Bas ?',
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
})
