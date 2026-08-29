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

  protected readonly timesUp = computed<boolean>(() => {
    const timeLeft = this.timeLeft()
    return timeLeft !== undefined && timeLeft <= 0
  })

  protected readonly showTimerWarning = computed<boolean>(() => {
    return this.timeLeft() <= this.#minSecondsWarning
      && this.timeLeft() > 0
  })
}
