import { computed, inject } from '@angular/core'

import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals'
import {
  catchError,
  lastValueFrom,
  of,
  switchMap,
  throwError,
} from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import {
  emptyRawQuezap, QuestionId, QuestionType, QuestionTypeFrom,
  QuezapId,
  QuezapWithQuestionsAndAnswers,
} from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../services'

type QuezapEditorStateModel = Omit<QuezapWithQuestionsAndAnswers, 'id'> & { id?: QuezapId }
type QuestionWithAnswers = QuezapWithQuestionsAndAnswers['questionWithAnswersAndResponses'][0]

const DEFAULT_QUESTION_TYPE = QuestionType.Quizz

function isQuestionAnswersValid(q: QuestionWithAnswers): boolean {
  if (q.type === QuestionType.Cash) {
    return q.answers.length >= 1
      && q.answers.some(a => a.isCorrect && Boolean(a.value?.trim()))
      && q.answers.every(a => Boolean(a.value?.trim()))
  }
  return q.answers.some(a => a.isCorrect)
}

function createEmptyQuezapState(type: QuestionType = DEFAULT_QUESTION_TYPE): QuezapEditorStateModel {
  const newQuestion = QuestionTypeFrom(type).getNewWithAnswers()
  const rawQuezap = emptyRawQuezap()
  return {
    ...rawQuezap,
    questionWithAnswersAndResponses: [
      ...rawQuezap.questionWithAnswersAndResponses,
      newQuestion,
    ],
  }
}

interface QuezapEditorState {
  quezap: QuezapEditorStateModel
  selectionQuestionIdx: number
  isDirty: boolean
  persisting: boolean
}

const initialState: QuezapEditorState = {
  quezap: createEmptyQuezapState(),
  selectionQuestionIdx: 0,
  isDirty: true,
  persisting: false,
}

export const QuezapEditorStore = signalStore(
  withState(initialState),

  withComputed(({ quezap, selectionQuestionIdx }) => ({
    questions: computed(() => quezap().questionWithAnswersAndResponses),
    selectedQuestion: computed(() =>
      quezap().questionWithAnswersAndResponses[selectionQuestionIdx()],
    ),
    hasInvalidAnswers: computed(() =>
      quezap().questionWithAnswersAndResponses.some(
        q => !isQuestionAnswersValid(q),
      ),
    ),
  })),

  withMethods((store, service = inject(QUEZAP_SERVICE)) => ({

    setQuezap(newQuezap: QuezapEditorStateModel) {
      patchState(store, {
        quezap: newQuezap,
        isDirty: false,
      })
    },

    updateTitle(title: string) {
      const trimmedTitle = title.trim()
      if (store.quezap().title === trimmedTitle) {
        return
      }
      patchState(store, state => ({
        quezap: { ...state.quezap, title: trimmedTitle },
        isDirty: true,
      }))
    },

    setSelectionQuestionIdx(idx: number) {
      patchState(store, { selectionQuestionIdx: idx })
    },

    addNewQuestion() {
      const newQuestion = QuestionTypeFrom(DEFAULT_QUESTION_TYPE).getNewWithAnswers()

      patchState(store, (state) => {
        const updatedQuestions = [...state.quezap.questionWithAnswersAndResponses, newQuestion]
        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: updatedQuestions },
          isDirty: true,
          selectionQuestionIdx: updatedQuestions.length - 1,
        }
      })
    },

    updateQuestionAtIdx(idx: number, updatedQuestion: QuestionWithAnswers) {
      patchState(store, (state) => {
        const updatedQuestions = [...state.quezap.questionWithAnswersAndResponses]
        updatedQuestions[idx] = updatedQuestion

        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: updatedQuestions },
          isDirty: true,
        }
      })
    },

    duplicateQuestionAtIdx(idx: number) {
      patchState(store, (state) => {
        const currentQuestions = state.quezap.questionWithAnswersAndResponses
        const questionToDuplicate = currentQuestions[idx]
        const duplicatedQuestion: QuestionWithAnswers = {
          ...questionToDuplicate,
          id: '' as QuestionId,
          answers: questionToDuplicate.answers.map(a => ({ ...a })),
          ...(questionToDuplicate.limit ? { limit: { ...questionToDuplicate.limit } } : {}),
        }
        const updatedQuestions = [
          ...currentQuestions.slice(0, idx + 1),
          duplicatedQuestion,
          ...currentQuestions.slice(idx + 1),
        ]

        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: updatedQuestions },
          isDirty: true,
          selectionQuestionIdx: idx + 1,
        }
      })
    },

    deleteQuestionAtIdx(idx: number) {
      const questions = store.questions()
      if (questions.length <= 1) {
        throw new Error('At least one question must exist')
      }

      patchState(store, (state) => {
        const updatedQuestions = state.quezap.questionWithAnswersAndResponses.filter(
          (_, qIdx) => qIdx !== idx,
        )

        const newIdx = Math.min(state.selectionQuestionIdx, updatedQuestions.length - 1)

        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: updatedQuestions },
          isDirty: true,
          selectionQuestionIdx: newIdx,
        }
      })
    },

    moveQuestionUp(idx: number) {
      if (idx <= 0) {
        return
      }

      patchState(store, (state) => {
        const questions = [...state.quezap.questionWithAnswersAndResponses]
        const temp = questions[idx]
        questions[idx] = questions[idx - 1]
        questions[idx - 1] = temp

        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: questions },
          isDirty: true,
          selectionQuestionIdx: idx - 1,
        }
      })
    },

    moveQuestionDown(idx: number) {
      const total = store.questions().length
      if (idx >= total - 1) {
        return
      }

      patchState(store, (state) => {
        const questions = [...state.quezap.questionWithAnswersAndResponses]
        const temp = questions[idx]
        questions[idx] = questions[idx + 1]
        questions[idx + 1] = temp

        return {
          quezap: { ...state.quezap, questionWithAnswersAndResponses: questions },
          isDirty: true,
          selectionQuestionIdx: idx + 1,
        }
      })
    },

    reset() {
      patchState(store, {
        quezap: createEmptyQuezapState(),
        isDirty: true,
        selectionQuestionIdx: 0,
      })
    },

    async persist(): Promise<QuezapWithQuestionsAndAnswers> {
      const currentQuezap = store.quezap()
      if (!currentQuezap.title.trim()) {
        throw new ValidationError(
          {},
          'Le titre du questionnaire ne peut pas être vide',
        )
      }

      const questions = store.questions()
      const hasEmptyQuestion = questions.some(q => !q.value.trim())
      if (hasEmptyQuestion) {
        throw new ValidationError(
          {},
          'Chaque question doit comporter un énoncé non vide',
        )
      }

      const hasUncheckedQuestion = questions.some(
        q => !isQuestionAnswersValid(q),
      )
      if (hasUncheckedQuestion) {
        throw new ValidationError(
          {},
          'Chaque question doit comporter au moins une bonne réponse valide',
        )
      }

      patchState(store, { persisting: true })

      return lastValueFrom(
        service.persistQuezap(store.quezap()).pipe(
          switchMap((output) => {
            if (isFailure(output)) {
              return throwError(() => output.error)
            }

            const fallbackQuezap: QuezapWithQuestionsAndAnswers = {
              ...store.quezap(),
              id: output.result.id,
            }

            if (typeof service.find !== 'function') {
              patchState(store, {
                quezap: fallbackQuezap,
                isDirty: false,
                persisting: false,
              })
              return of(fallbackQuezap)
            }

            return service.find(output.result.id).pipe(
              switchMap((findOutput) => {
                const savedQuezap = isFailure(findOutput) ? fallbackQuezap : findOutput.result
                patchState(store, {
                  quezap: savedQuezap,
                  isDirty: false,
                  persisting: false,
                })
                return of(savedQuezap)
              }),
              catchError(() => {
                patchState(store, {
                  quezap: fallbackQuezap,
                  isDirty: false,
                  persisting: false,
                })
                return of(fallbackQuezap)
              }),
            )
          }),
          catchError((err) => {
            patchState(store, { persisting: false })
            return throwError(() => err)
          }),
        ),
      )
    },
  })),
)
