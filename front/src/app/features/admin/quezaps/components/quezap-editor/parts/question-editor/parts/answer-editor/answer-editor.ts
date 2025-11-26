import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { QuestionType } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../../../editor-container'

import { AnswerComponent } from './parts'

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
        return this.selectedQuestion().answers.slice(0, 2)
      case QuestionType.Binary:
        return this.selectedQuestion().answers.slice(0, 2)
      case QuestionType.Quizz:
      default:
        return this.selectedQuestion().answers
    }
  })
}
