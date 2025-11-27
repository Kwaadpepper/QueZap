import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { Question } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../../../editor-container'

export type QuestionTimerInput = Pick<Question, 'limit'>

@Component({
  selector: 'quizz-question-timer',
  templateUrl: './question-timer.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTimer {
  private readonly editorContainer = inject(QuezapEditorContainer)
  protected readonly selectedQuestion = computed(() => this.editorContainer.selectedQuestion())

  protected readonly timer = computed(() => this.selectedQuestion().limit)
  protected readonly remainingSeconds = computed<number>(() => this.timer()?.seconds ?? -1)
  protected readonly remainingSecondsText = computed(() => this.timer()?.seconds ?? '∞')
}
