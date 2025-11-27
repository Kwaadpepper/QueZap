import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import {
  catchError, finalize, of, switchMap, tap, throwError,
} from 'rxjs'

import { isFailure } from '@quezap/core/types'
import {
  emptyRawQuezap, QuestionType, QuestionTypeFrom,
  QuezapWithQuestionsAndAnswers,
} from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../../services'

type QuezapEditorState = Omit<QuezapWithQuestionsAndAnswers, 'id'>
type QuestionWithAnswers = QuezapWithQuestionsAndAnswers['questionWithAnswersAndResponses'][0]

@Injectable()
export class QuezapEditorContainer {
  private readonly destroyRef = inject(DestroyRef)
  private readonly quezapService = inject(QUEZAP_SERVICE)
  readonly #defautQuestionType = QuestionType.Quizz

  private readonly _quezap = signal<QuezapEditorState>(
    this.createEmptyQuestion(this.#defautQuestionType),
  )

  private readonly _selectionQuestionIdx = signal<number>(0)

  readonly quezap = this._quezap.asReadonly()
  readonly questions = computed(() => this._quezap().questionWithAnswersAndResponses)
  readonly selectionQuestionIdx = this._selectionQuestionIdx.asReadonly()
  readonly selectedQuestion = computed(() =>
    this._quezap().questionWithAnswersAndResponses[
      this._selectionQuestionIdx()
    ],
  )

  private readonly _isDirty = signal<boolean>(false)
  readonly isDirty = this._isDirty.asReadonly()

  private readonly _persisting = signal<boolean>(false)
  readonly persisting = this._persisting.asReadonly()

  public persist(): Promise<QuezapWithQuestionsAndAnswers> {
    this._persisting.update(() => true)

    return new Promise((resolve, reject) => {
      this.quezapService.persistQuezap(this.quezap()).pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap((output) => {
          if (isFailure(output)) {
            const err = output.error
            return throwError(() => err)
          }

          this._isDirty.set(false)

          return of({
            id: output.result.id,
            ...this._quezap(),
          })
        }),
        tap(value => resolve(value)),
        catchError((err) => {
          this._persisting.set(false)
          reject(err)
          return of(void 0)
        }),
        finalize(() => this._persisting.set(false)),
      ).subscribe()
    })
  }

  public setQuezap(quezap: QuezapEditorState) {
    this._quezap.set(quezap)
    this._isDirty.set(false)
  }

  public setSelectionQuestionIdx(idx: number) {
    this._selectionQuestionIdx.set(idx)
  }

  public addNewQuestion() {
    const currentQuezap = this._quezap()
    const newQuestion = QuestionTypeFrom(this.#defautQuestionType).getNewWithAnswers()
    const updatedQuestions = [
      ...currentQuezap.questionWithAnswersAndResponses,
      newQuestion,
    ]

    this._quezap.set({
      ...currentQuezap,
      questionWithAnswersAndResponses: updatedQuestions,
    })
    this.markAsDirty()
    this.setSelectionQuestionIdx(updatedQuestions.length - 1)
  }

  public updateQuestionAtIdx(idx: number, updatedQuestion: QuestionWithAnswers) {
    const currentQuezap = this._quezap()
    const updatedQuestions = [...currentQuezap.questionWithAnswersAndResponses]
    updatedQuestions[idx] = updatedQuestion

    this._quezap.set({
      ...currentQuezap,
      questionWithAnswersAndResponses: updatedQuestions,
    })
    this.markAsDirty()
  }

  public duplicateQuestionAtIdx(idx: number) {
    const currentQuezap = this._quezap()
    const questionToDuplicate = currentQuezap.questionWithAnswersAndResponses[idx]
    const updatedQuestions = [
      ...currentQuezap.questionWithAnswersAndResponses.slice(0, idx + 1),
      { ...questionToDuplicate },
      ...currentQuezap.questionWithAnswersAndResponses.slice(idx + 1),
    ]

    this._quezap.set({
      ...currentQuezap,
      questionWithAnswersAndResponses: updatedQuestions,
    })
    this.markAsDirty()
    this.setSelectionQuestionIdx(idx + 1)
  }

  public deleteQuestionAtIdx(idx: number) {
    if (this.questions().length <= 1) {
      throw new Error('At least one question must exist')
    }

    const currentQuezap = this._quezap()
    const updatedQuestions = currentQuezap.questionWithAnswersAndResponses.filter(
      (_, questionIdx) => questionIdx !== idx,
    )

    this._quezap.set({
      ...currentQuezap,
      questionWithAnswersAndResponses: updatedQuestions,
    })
    this.markAsDirty()
    this._selectionQuestionIdx.set(
      Math.min(this._selectionQuestionIdx(), updatedQuestions.length - 1),
    )
  }

  public reset() {
    this._quezap.set(
      this.createEmptyQuestion(this.#defautQuestionType),
    )
    this.markAsDirty()
    this._selectionQuestionIdx.set(0)
  }

  private markAsDirty() {
    this._isDirty.set(true)
  }

  private createEmptyQuestion(type: QuestionType): QuezapEditorState {
    const newQuestion = QuestionTypeFrom(type).getNewWithAnswers()
    const rawQuezap = emptyRawQuezap()
    const newQuezap = {
      ...rawQuezap,
      questionWithAnswersAndResponses: [
        ...rawQuezap.questionWithAnswersAndResponses,
        newQuestion,
      ],
    }

    setTimeout(() => {
      this.markAsDirty()
    })

    return newQuezap
  }
}
