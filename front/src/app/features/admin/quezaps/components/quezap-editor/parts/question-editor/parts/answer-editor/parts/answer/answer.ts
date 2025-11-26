import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'

import { QuezapEditorContainer } from '@quezap/features/admin/quezaps/components/quezap-editor/editor-container'
import { QuestionIcon, QuestionIconType } from '@quezap/features/quizz/components/question-view/parts'

@Component({
  selector: 'quizz-answer',
  templateUrl: './answer.html',
  imports: [QuestionIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerComponent {
  private readonly editorContainer = inject(QuezapEditorContainer)

  private readonly answers = computed(() => this.editorContainer.selectedQuestion().answers)
  private readonly answer = computed(() => this.answers()[this.index()])
  protected readonly phrase = computed(() => this.answer().value ?? '')

  readonly index = input<number>(0)

  protected readonly currentForm = computed(() => Object.values(QuestionIconType)[this.index()])
}
