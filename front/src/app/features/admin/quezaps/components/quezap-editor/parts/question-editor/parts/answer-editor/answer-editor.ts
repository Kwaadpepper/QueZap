import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'

import { getNewAnswerWithResponse, QuestionType } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../../../editor-container'

import { AnswerComponent, AnswerInput } from './parts'

@Component({
  selector: 'quizz-answer-editor',
  templateUrl: './answer-editor.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AnswerComponent],
})
export class AnswerEditor {
  private readonly editorContainer = inject(QuezapEditorContainer)

  // * This is used for toggling between first and second answer in boolean or binary questions
  private readonly booleanToggle = signal(0)

  protected readonly selectedQuestion = computed(() => this.editorContainer.selectedQuestion())
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
      case QuestionType.Quizz:
      default:
        return this.selectedQuestion().answers
    }
  })

  protected readonly answersAreReadonly = computed(() => this.selectedQuestion().type === QuestionType.Boolean)

  protected onAnswerChanged(updatedAnswer: AnswerInput) {
    const answers = this.selectedQuestion().answers
    let updatedAnswers = answers.map(answer =>
      answer.index === updatedAnswer.index ? updatedAnswer : answer,
    )

    // * Handle boolean behavior for correctness
    if (this.selectedQuestion().type !== QuestionType.Quizz) {
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
          // * Change only correctness
          answers: updatedAnswers.map((a, idx) => ({
            ...this.selectedQuestion().answers[idx],
            isCorrect: a.isCorrect,
          })),
        }
      : {
          ...this.selectedQuestion(),
          // * Update all values
          answers: updatedAnswers,
        }

    this.editorContainer.updateQuestionAtIdx(
      this.editorContainer.selectionQuestionIdx(),
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
