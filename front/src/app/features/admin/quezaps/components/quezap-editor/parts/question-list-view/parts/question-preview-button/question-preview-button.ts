import {
  Component, computed,
  inject, input,
  output,
} from '@angular/core'

import { BadgeModule } from 'primeng/badge'
import { TagModule } from 'primeng/tag'

import { QuestionType, QuestionTypeFrom, QuestionWithAnswersAndResponses } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes/minutes'

import { QuezapEditorContainer } from '../../../../editor-container'

type QuestionPreviewButtonInput = Omit<QuestionWithAnswersAndResponses, 'id'>

@Component({
  selector: 'quizz-question-preview-button',
  imports: [
    BadgeModule,
    TagModule,
    MinutesPipe,
  ],
  templateUrl: './question-preview-button.html',
  styleUrl: './question-preview-button.css',
})
export class QuestionPreviewButton {
  private readonly editorContainer = inject(QuezapEditorContainer)
  readonly question = input.required<QuestionPreviewButtonInput>()
  readonly index = input.required<number>()
  readonly selected = output()

  protected readonly _selected = computed(() => this.editorContainer.selectionQuestionIdx() === this.index())
  protected readonly type = computed(() => QuestionTypeFrom(this.question().type).toString())
  protected readonly answers = computed(() => {
    switch (this.question().type) {
      case QuestionType.Binary:
        return this.question().answers.slice(0, 2)
      case QuestionType.Boolean:
        return this.question().answers.slice(0, 2).map(a => ({
          ...a,
          value: '',
        }))
      case QuestionType.Quizz:
      default:
        return this.question().answers
    }
  })

  protected onSelectQuestion() {
    this.selected.emit()
  }
}
