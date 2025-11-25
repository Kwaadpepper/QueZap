import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

import { Question } from '@quezap/domain/models'

export type QuestionTimerInput = Pick<Question, 'limit'>

@Component({
  selector: 'quizz-question-timer',
  templateUrl: './question-timer.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTimer {
  readonly question = input.required<QuestionTimerInput>()

  protected readonly timer = computed(() => this.question().limit)
  protected readonly remainingSeconds = computed<number>(() => this.timer()?.seconds ?? -1)
  protected readonly remainingSecondsText = computed(() => this.timer()?.seconds ?? '∞')
}
