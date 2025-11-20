import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core'
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop'

import { concatMap, filter, interval, take, tap } from 'rxjs'

import { Timer } from '@quezap/domain/models'

@Component({
  selector: 'quizz-timer',
  imports: [],
  templateUrl: './question-timer.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTimer {
  /** The decount start signal */
  readonly started = input.required<boolean>()
  /** Timer in seconds */
  readonly timer = input.required<Timer>()
  /** When timer reaches 0 */
  readonly exhausted = output<boolean>()

  readonly limit = computed<number>(() => this.timer().seconds)
  protected readonly remainingSeconds = signal<number>(0)

  constructor() {
    effect(() => {
      if (this.started()) {
        this.remainingSeconds.set(this.limit())
      }
    })

    toObservable(this.started).pipe(
      takeUntilDestroyed(),
      filter(started => started),
      take(1),
      // Initialize remaining seconds
      tap(() => this.remainingSeconds.set(this.limit())),
      // Tick every second
      concatMap(() => interval(1000).pipe(
        take(this.limit()),
        tap(() => this.remainingSeconds.update(seconds => seconds - 1)),
      )),
    ).subscribe({
      complete: () => this.exhausted.emit(true),
    })
  }
}
