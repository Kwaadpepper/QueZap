import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'

import { getNewAnswerWithResponse, QuestionType } from '@quezap/domain/models'

import { QuezapEditorStore } from '../../../../../../stores'

import { AnswerComponent, AnswerInput } from './parts'

@Component({
  selector: 'quizz-answer-editor',
  templateUrl: './answer-editor.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnswerComponent],
})
export class AnswerEditor {
  private readonly editorStore = inject(QuezapEditorStore)

  // * This is used for toggling between first and second answer in boolean or binary questions
  private readonly booleanToggle = signal(0)

  protected readonly selectedQuestion = computed(() => this.editorStore.selectedQuestion())
  protected readonly isCash = computed(() => this.selectedQuestion().type === QuestionType.Cash)

  // * This is used for displaying answers in the editor
  protected readonly answers = computed(() => {
    switch (this.selectedQuestion().type) {
      case QuestionType.Boolean:
        return [
          {
            ...getNewAnswerWithResponse(0),
            ...this.selectedQuestion().answers.slice(0, 1)[0],
            value: 'Vrai',
          },
          {
            ...getNewAnswerWithResponse(1),
            ...this.selectedQuestion().answers.slice(1, 2)[0],
            value: 'Faux',
          },
        ]
      case QuestionType.Binary:
        return this.selectedQuestion().answers.slice(0, 2)
      case QuestionType.Cash:
        return this.selectedQuestion().answers
      case QuestionType.Quizz:
      default:
        return this.selectedQuestion().answers
    }
  })

  protected readonly answersAreReadonly = computed(() => this.selectedQuestion().type === QuestionType.Boolean)

  protected readonly canAddAnswer = computed(() => {
    const type = this.selectedQuestion().type
    if (type === QuestionType.Quizz || type === QuestionType.Cash) {
      return this.selectedQuestion().answers.length < 4
    }
    return false
  })

  protected readonly canRemoveAnswer = computed(() => {
    const type = this.selectedQuestion().type
    if (type === QuestionType.Quizz) {
      return this.selectedQuestion().answers.length > 2
    }
    if (type === QuestionType.Cash) {
      return this.selectedQuestion().answers.length > 1
    }
    return false
  })

  protected addAnswer() {
    if (!this.canAddAnswer()) {
      return
    }
    const currentQuestion = this.selectedQuestion()
    const nextIndex = currentQuestion.answers.length
    const isCash = currentQuestion.type === QuestionType.Cash
    const updatedAnswers = [
      ...currentQuestion.answers,
      {
        ...getNewAnswerWithResponse(nextIndex),
        isCorrect: isCash ? true : false,
        picture: undefined,
      },
    ]
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      {
        ...currentQuestion,
        answers: updatedAnswers,
      },
    )
  }

  protected removeAnswer(indexToRemove: number) {
    if (!this.canRemoveAnswer()) {
      return
    }
    const currentQuestion = this.selectedQuestion()
    const isCash = currentQuestion.type === QuestionType.Cash
    const filteredAnswers = currentQuestion.answers.filter((_, idx) => idx !== indexToRemove)
    // Re-index remaining answers
    const reindexedAnswers = filteredAnswers.map((answer, idx) => ({
      ...answer,
      index: idx,
      isCorrect: isCash ? true : answer.isCorrect,
      picture: isCash ? undefined : answer.picture,
    }))
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      {
        ...currentQuestion,
        answers: reindexedAnswers,
      },
    )
  }

  protected onCashAnswerTextChange(index: number, event: Event) {
    const target = event.target as HTMLInputElement
    const newText = target.value
    const currentQuestion = this.selectedQuestion()
    const updatedAnswers = currentQuestion.answers.map((answer, idx) => {
      if (idx === index) {
        return {
          ...answer,
          value: newText,
          isCorrect: true,
          picture: undefined,
        }
      }
      return {
        ...answer,
        isCorrect: true,
        picture: undefined,
      }
    })
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      {
        ...currentQuestion,
        answers: updatedAnswers,
      },
    )
  }

  protected onAnswerChanged(updatedAnswer: AnswerInput) {
    if (this.selectedQuestion().type === QuestionType.Cash) {
      const updatedAnswers = this.selectedQuestion().answers.map(answer =>
        answer.index === updatedAnswer.index
          ? { ...updatedAnswer, isCorrect: true, picture: undefined }
          : { ...answer, isCorrect: true, picture: undefined },
      )
      this.editorStore.updateQuestionAtIdx(
        this.editorStore.selectionQuestionIdx(),
        {
          ...this.selectedQuestion(),
          answers: updatedAnswers,
        },
      )
      return
    }

    const answers = this.selectedQuestion().type === QuestionType.Boolean
      ? this.answers()
      : this.selectedQuestion().answers
    const previousAnswer = answers.find(answer => answer.index === updatedAnswer.index)
    const correctnessChanged = previousAnswer?.isCorrect !== updatedAnswer.isCorrect

    let updatedAnswers = answers.map(answer =>
      answer.index === updatedAnswer.index ? updatedAnswer : answer,
    )

    // * Handle boolean behavior for correctness only when correctness is toggled
    if (this.selectedQuestion().type !== QuestionType.Quizz && correctnessChanged) {
      this.booleanToggle.update(() => {
        if (updatedAnswer.index === 0) {
          return updatedAnswer.isCorrect ? 0 : 1
        }
        return updatedAnswer.isCorrect ? 1 : 0
      })
      updatedAnswers = this.makeSureFirstOrSecondIsCorrect(updatedAnswers)
    }

    const updatedQuestion = this.selectedQuestion().type === QuestionType.Boolean
      ? {
          ...this.selectedQuestion(),
          // * Change only correctness and picture
          answers: updatedAnswers.map((a, idx) => ({
            ...(this.selectedQuestion().answers[idx] ?? answers[idx]),
            index: idx,
            value: idx === 0 ? 'Vrai' : 'Faux',
            isCorrect: a.isCorrect,
            picture: a.picture,
          })),
        }
      : {
          ...this.selectedQuestion(),
          // * Update all values
          answers: updatedAnswers,
        }

    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      updatedQuestion,
    )
  }

  private makeSureFirstOrSecondIsCorrect(updatedAnswers: AnswerInput[]): AnswerInput[] {
    return updatedAnswers.map(answer => ({
      ...answer,
      isCorrect: answer.index === this.booleanToggle(),
    }))
  }
}
