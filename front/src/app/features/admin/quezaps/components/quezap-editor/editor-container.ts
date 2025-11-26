import { computed, Injectable, signal } from '@angular/core'

import { emptyRawQuezap, QuestionType, QuestionTypeFrom, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

type QuezapEditorState = Omit<QuezapWithQuestionsAndAnswers, 'id'>
type QuestionWithAnswers = QuezapWithQuestionsAndAnswers['questionWithAnswersAndResponses'][0]

@Injectable()
export class QuezapEditorContainer {
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

  readonly isDirty = signal<boolean>(false)

  public setQuezap(quezap: QuezapEditorState) {
    this._quezap.set(quezap)
    this.isDirty.set(false)
  }

  public setSelectionQuestionIdx(idx: number) {
    this._selectionQuestionIdx.set(idx)
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

  public reset() {
    this._quezap.set(
      this.createEmptyQuestion(this.#defautQuestionType),
    )
    this.markAsDirty()
  }

  private markAsDirty() {
    this.isDirty.set(true)
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
      this.setSelectionQuestionIdx(0)
    })

    return newQuezap
  }
}
