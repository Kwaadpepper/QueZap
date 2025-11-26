import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

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

  protected readonly answers = computed(() => this.editorContainer.selectedQuestion().answers)
}
