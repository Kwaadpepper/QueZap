import { TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { QuezapId } from '@quezap/domain/models'

import { QUEZAP_SERVICE, QuezapService } from '../services'

import { QuezapEditorStore } from './editor-store'

describe('QuezapEditorStore', () => {
  let store: InstanceType<typeof QuezapEditorStore>
  let mockService: Partial<QuezapService>

  beforeEach(() => {
    mockService = {
      persistQuezap: jest.fn().mockReturnValue(
        of({
          kind: 'success',
          result: { id: '018e3a2b-1111-7def-8123-abcdef111111' as QuezapId },
        }),
      ),
    }

    TestBed.configureTestingModule({
      providers: [
        QuezapEditorStore,
        { provide: QUEZAP_SERVICE, useValue: mockService },
      ],
    })

    store = TestBed.inject(QuezapEditorStore)
  })

  it('should initialize with 1 question and invalid answers', () => {
    expect(store.questions().length).toBe(1)
    expect(store.hasInvalidAnswers()).toBe(true)
  })

  it('should reorder questions with moveQuestionUp and moveQuestionDown', () => {
    store.addNewQuestion()
    expect(store.questions().length).toBe(2)

    const q0 = store.questions()[0]
    const q1 = store.questions()[1]

    store.moveQuestionUp(1)
    expect(store.questions()[0]).toBe(q1)
    expect(store.questions()[1]).toBe(q0)
    expect(store.selectionQuestionIdx()).toBe(0)

    store.moveQuestionDown(0)
    expect(store.questions()[0]).toBe(q0)
    expect(store.questions()[1]).toBe(q1)
    expect(store.selectionQuestionIdx()).toBe(1)
  })

  it('should detect when at least one question has a correct answer', () => {
    const question = store.questions()[0]
    const updatedQuestion = {
      ...question,
      answers: question.answers.map((a, i) =>
        i === 0 ? { ...a, isCorrect: true } : a,
      ),
    }

    store.updateQuestionAtIdx(0, updatedQuestion)
    expect(store.hasInvalidAnswers()).toBe(false)
  })

  it('should update title and mark store as dirty', () => {
    store.setQuezap({
      title: 'Initial Title',
      description: '',
      questionWithAnswersAndResponses: store.questions(),
    })
    expect(store.isDirty()).toBe(false)

    store.updateTitle('  Updated Title  ')
    expect(store.quezap().title).toBe('Updated Title')
    expect(store.isDirty()).toBe(true)
  })

  it('should not mark store as dirty if title is unchanged after trim', () => {
    store.setQuezap({
      title: 'Same Title',
      description: '',
      questionWithAnswersAndResponses: store.questions(),
    })
    expect(store.isDirty()).toBe(false)

    store.updateTitle('  Same Title  ')
    expect(store.isDirty()).toBe(false)
  })

  it('should reject persist if quiz title is empty', async () => {
    const question = store.questions()[0]
    store.updateQuestionAtIdx(0, {
      ...question,
      value: 'Une question ?',
      answers: question.answers.map((a, i) => (i === 0 ? { ...a, isCorrect: true } : a)),
    })

    await expect(store.persist()).rejects.toThrow(ValidationError)
    expect(mockService.persistQuezap).not.toHaveBeenCalled()
  })

  it('should reject persist if a question statement is empty', async () => {
    store.updateTitle('Quiz valide')
    const question = store.questions()[0]
    store.updateQuestionAtIdx(0, {
      ...question,
      value: '   ',
      answers: question.answers.map((a, i) => (i === 0 ? { ...a, isCorrect: true } : a)),
    })

    await expect(store.persist()).rejects.toThrow(ValidationError)
    expect(mockService.persistQuezap).not.toHaveBeenCalled()
  })

  it('should reject persist if a question has no correct answers', async () => {
    store.updateTitle('Quiz valide')
    const question = store.questions()[0]
    store.updateQuestionAtIdx(0, {
      ...question,
      value: 'Une question ?',
    })

    await expect(store.persist()).rejects.toThrow(ValidationError)
    expect(mockService.persistQuezap).not.toHaveBeenCalled()
  })

  it('should successfully persist when title, question statements and answers are valid', async () => {
    store.updateTitle('Quiz valide')
    const question = store.questions()[0]
    const updatedQuestion = {
      ...question,
      value: 'Quelle est la bonne réponse ?',
      answers: question.answers.map((a, i) =>
        i === 0 ? { ...a, isCorrect: true } : a,
      ),
    }
    store.updateQuestionAtIdx(0, updatedQuestion)

    const result = await store.persist()
    expect(result.id).toBe('018e3a2b-1111-7def-8123-abcdef111111')
    expect(mockService.persistQuezap).toHaveBeenCalled()
    expect(store.isDirty()).toBe(false)
  })

  it('should refresh persisted question pictures from service.find when available', async () => {
    const savedId = '018e3a2b-1111-7def-8123-abcdef111111' as QuezapId
    store.updateTitle('Quiz avec image')
    const question = store.questions()[0]
    store.updateQuestionAtIdx(0, {
      ...question,
      value: 'Quel est ce monument ?',
      picture: 'data:image/png;base64,iVBORw0KGgo=' as never,
      answers: question.answers.map((a, i) =>
        i === 0 ? { ...a, value: 'Tour Eiffel', isCorrect: true } : { ...a, value: `Option ${i}` },
      ),
    })

    mockService.find = jest.fn().mockReturnValue(
      of({
        kind: 'success',
        result: {
          id: savedId,
          title: 'Quiz avec image',
          description: '',
          questionWithAnswersAndResponses: [
            {
              ...store.questions()[0],
              picture: '/quezap-public-images/pictures/questions/saved.png' as never,
            },
          ],
        },
      }),
    )

    const result = await store.persist()
    expect(mockService.find).toHaveBeenCalledWith(savedId)
    expect(result.questionWithAnswersAndResponses[0].picture).toBe(
      '/quezap-public-images/pictures/questions/saved.png',
    )
    expect(store.questions()[0].picture).toBe(
      '/quezap-public-images/pictures/questions/saved.png',
    )
  })
})
