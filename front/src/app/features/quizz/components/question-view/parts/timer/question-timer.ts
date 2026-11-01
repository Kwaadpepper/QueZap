import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  output,
  signal,
} from '@angular/core'
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop'

import {
  filter, interval, switchMap,
  takeWhile, tap,
} from 'rxjs'

import { Timer } from '@quezap/domain/models'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

@Component({
  selector: 'quizz-timer',
  imports: [],
  templateUrl: './question-timer.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTimer {
  private readonly questionStore = inject(TimerStore)
  private readonly sessionStore = inject(ActiveSessionStore)

  readonly exhausted = output<void>()

  protected readonly isPaused = computed<boolean>(() => this.sessionStore.isSessionPaused())

  /** The decount start signal */
  readonly started = computed<boolean>(() => this.questionStore.started())
  /** Timer in seconds */
  readonly timer = computed<Timer | undefined>(() => this.questionStore.timer())

  readonly limit = computed<number>(() => this.timer()?.seconds ?? 0)
  protected readonly remainingSeconds = signal<number>(0)

  protected readonly circumference = 2 * Math.PI * 26 // ~163.36 for r=26

  protected readonly strokeDashoffset = computed<number>(() => {
    const total = this.limit()
    if (total <= 0) return 0
    const progress = this.remainingSeconds() / total
    return this.circumference * (1 - Math.max(0, Math.min(1, progress)))
  })

  protected readonly isUrgent = computed<boolean>(() => this.remainingSeconds() <= 5 && this.remainingSeconds() > 0)

  protected readonly strokeColor = computed<string>(() => {
    if (this.isUrgent()) return '#ef4444'
    if (this.remainingSeconds() <= 15) return '#f59e0b'
    return '#10b981'
  })

  constructor() {
    effect(() => {
      if (this.started()) {
        const deadline = this.timer()?.deadline
        if (deadline) {
          const remaining = Math.max(0, Math.ceil((new Date(deadline).getTime() - Date.now()) / 1000))
          this.remainingSeconds.set(remaining)
        }
        else {
          this.remainingSeconds.set(this.limit())
        }
      }
    })

    toObservable(this.started).pipe(
      takeUntilDestroyed(),
      filter(started => started),
      tap(() => {
        const deadline = this.timer()?.deadline
        if (deadline) {
          const remaining = Math.max(0, Math.ceil((new Date(deadline).getTime() - Date.now()) / 1000))
          this.remainingSeconds.set(remaining)
        }
        else {
          this.remainingSeconds.set(this.limit())
        }
      }),
      switchMap(() => {
        return interval(1000).pipe(
          takeWhile(() => this.remainingSeconds() > 0 && this.questionStore.started()),
          filter(() => !this.isPaused()),
          tap(() => {
            this.remainingSeconds.update(seconds => Math.max(0, seconds - 1))
          }),
          tap(() => this.questionStore.setTimeLeft(this.remainingSeconds())),
          tap(() => {
            if (this.remainingSeconds() === 0) {
              this.exhausted.emit()
            }
          }),
        )
      }),
    ).subscribe()
  }
}
