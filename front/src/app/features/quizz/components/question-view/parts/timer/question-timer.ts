import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  signal,
} from '@angular/core'
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop'

import {
  filter, interval, switchMap,
  takeWhile, tap,
} from 'rxjs'

import { Timer } from '@quezap/domain/models'
import { TimerStore } from '@quezap/features/quizz/stores'

@Component({
  selector: 'quizz-timer',
  imports: [],
  templateUrl: './question-timer.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTimer {
  private readonly questionStore = inject(TimerStore)

  /** The decount start signal */
  readonly started = computed<boolean>(() => this.questionStore.started())
  /** Timer in seconds */
  readonly timer = computed<Timer | undefined>(() => this.questionStore.timer())

  readonly limit = computed<number>(() => this.timer()?.seconds ?? 0)
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
      // Initialize remaining seconds
      tap(() => this.remainingSeconds.set(this.limit())),
      // Tick every second
      switchMap(() => interval(1000).pipe(
        takeWhile(() => this.remainingSeconds() > 0 && this.questionStore.started()),
        tap(() => this.remainingSeconds.update(seconds => seconds - 1)),
        tap(() => this.questionStore.setTimeLeft(this.remainingSeconds())),
      )),
    ).subscribe()
  }
}
