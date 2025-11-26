import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

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

  protected readonly selectedQuestion = computed(() => this.editorContainer.selectedQuestion())
  protected readonly answers = computed(() => {
    switch (this.selectedQuestion().type) {
      case QuestionType.Boolean:
        return [
          {
            ...getNewAnswerWithResponse(0),
            isCorrect: true,
            value: 'Vrai',
          },
          {
            ...getNewAnswerWithResponse(1),
            isCorrect: false,
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
    const updatedAnswers = this.answers().map(answer =>
      answer.index === updatedAnswer.index ? updatedAnswer : answer,
    )

    const updatedQuestion = {
      ...this.selectedQuestion(),
      answers: updatedAnswers,
    }

    this.editorContainer.updateQuestionAtIdx(
      this.editorContainer.selectionQuestionIdx(),
      updatedQuestion,
    )
  }
}
