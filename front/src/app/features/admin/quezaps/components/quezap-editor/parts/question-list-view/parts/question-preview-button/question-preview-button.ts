import {
  Component, computed,
  inject, input,
  output,
} from '@angular/core'

import { BadgeModule } from 'primeng/badge'
import { TagModule } from 'primeng/tag'

import { QuestionType, QuestionTypeFrom, QuestionWithAnswersAndResponses } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes/minutes'

import { QuezapEditorStore } from '../../../../../../stores'

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
  private readonly editorStore = inject(QuezapEditorStore)
  readonly question = input.required<QuestionPreviewButtonInput>()
  readonly index = input.required<number>()
  readonly selected = output()

  protected readonly _selected = computed(() => this.editorStore.selectionQuestionIdx() === this.index())
  protected readonly type = computed(() => QuestionTypeFrom(this.question().type).toString())
  protected readonly answers = computed(() => {
    switch (this.question().type) {
      case QuestionType.Binary:
        return this.question().answers.slice(0, 2)
      case QuestionType.Boolean:
        return this.question().answers.slice(0, 2).map((a, idx) => ({
          ...a,
          value: idx === 0 ? 'Vrai' : 'Faux',
        }))
      case QuestionType.Quizz:
      default:
        return this.question().answers
    }
  })

  protected readonly hasCorrectAnswer = computed(() => {
    const q = this.question()
    if (q.type === QuestionType.Cash) {
      return q.answers.length >= 1
        && q.answers.some(a => a.isCorrect && Boolean(a.value?.trim()))
        && q.answers.every(a => Boolean(a.value?.trim()))
    }
    return q.answers.some(a => a.isCorrect)
  })

  protected onSelectQuestion() {
    this.selected.emit()
  }
}
