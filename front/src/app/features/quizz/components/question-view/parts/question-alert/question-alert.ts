import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

@Component({
  selector: 'quizz-question-alert',
  imports: [],
  templateUrl: './question-alert.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionAlert {
  readonly #minSecondsWarning = 15
  readonly timeLeft = input.required<number>()
  readonly isRevealing = input<boolean>(false)

  protected readonly timesUp = computed<boolean>(() => {
    if (this.isRevealing()) return false
    const timeLeft = this.timeLeft()
    return timeLeft !== undefined && timeLeft <= 0
  })

  protected readonly showTimerWarning = computed<boolean>(() => {
    if (this.isRevealing()) return false
    return this.timeLeft() <= this.#minSecondsWarning
      && this.timeLeft() > 0
  })
}
