import { ComponentFixture, TestBed } from '@angular/core/testing'

import { QuestionId, QuestionType } from '@quezap/domain/models'
import { QuezapEditorStore } from '@quezap/features/admin/quezaps/stores'

import { AnswerEditor } from './answer-editor'

describe('AnswerEditor', () => {
  let component: AnswerEditor
  let fixture: ComponentFixture<AnswerEditor>
  let store: InstanceType<typeof QuezapEditorStore>

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnswerEditor],
      providers: [QuezapEditorStore],
    }).compileComponents()

    store = TestBed.inject(QuezapEditorStore)
    fixture = TestBed.createComponent(AnswerEditor)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should not flip correctness when editing text of 2nd option in a binary question', () => {
    store.updateQuestionAtIdx(0, {
      id: '' as QuestionId,
      value: 'Question binaire',
      type: QuestionType.Binary,
      answers: [
        { index: 0, points: 1000, value: 'Option 1', isCorrect: false },
        { index: 1, points: 1000, value: 'Option 2', isCorrect: false },
      ],
    })

    // Edit text of 2nd option without checking isCorrect
    component['onAnswerChanged']({
      index: 1,
      points: 1000,
      value: 'Option 2 modifiée',
      isCorrect: false,
    })

    const answersAfterTextEdit = store.selectedQuestion().answers
    expect(answersAfterTextEdit[0].isCorrect).toBe(false)
    expect(answersAfterTextEdit[1].isCorrect).toBe(false)
    expect(answersAfterTextEdit[1].value).toBe('Option 2 modifiée')

    // Now check option 0 as correct
    component['onAnswerChanged']({
      index: 0,
      points: 1000,
      value: 'Option 1',
      isCorrect: true,
    })

    expect(store.selectedQuestion().answers[0].isCorrect).toBe(true)
    expect(store.selectedQuestion().answers[1].isCorrect).toBe(false)

    // Edit text of option 1 while option 0 is correct -> option 0 must stay correct
    component['onAnswerChanged']({
      index: 1,
      points: 1000,
      value: 'Option 2 encore modifiée',
      isCorrect: false,
    })

    expect(store.selectedQuestion().answers[0].isCorrect).toBe(true)
    expect(store.selectedQuestion().answers[1].isCorrect).toBe(false)
    expect(store.selectedQuestion().answers[1].value).toBe('Option 2 encore modifiée')
  })

  it('should handle cash question variants and ensure all answers remain correct', () => {
    store.updateQuestionAtIdx(0, {
      id: '' as QuestionId,
      value: 'Quelle fleur symbole des Pays-Bas ?',
      type: QuestionType.Cash,
      answers: [
        { index: 0, points: 1000, value: 'Tulipe', isCorrect: true },
      ],
    })

    expect(component['isCash']()).toBe(true)
    expect(component['canAddAnswer']()).toBe(true)
    expect(component['canRemoveAnswer']()).toBe(false)

    // Add variant
    component['addAnswer']()
    expect(store.selectedQuestion().answers.length).toBe(2)
    expect(store.selectedQuestion().answers[1].isCorrect).toBe(true)
    expect(component['canRemoveAnswer']()).toBe(true)

    // Edit variant text
    const mockInputEvent = { target: { value: 'Tulipe rouge' } } as unknown as Event
    component['onCashAnswerTextChange'](1, mockInputEvent)
    expect(store.selectedQuestion().answers[1].value).toBe('Tulipe rouge')
    expect(store.selectedQuestion().answers[1].isCorrect).toBe(true)

    // Remove variant
    component['removeAnswer'](1)
    expect(store.selectedQuestion().answers.length).toBe(1)
    expect(store.selectedQuestion().answers[0].value).toBe('Tulipe')
    expect(component['canRemoveAnswer']()).toBe(false)
  })
})
