import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core'

import { BehaviorSubject, EMPTY, interval, switchMap, takeWhile, tap } from 'rxjs'

import { Easing, ProgressionEase } from './easing'

@Injectable({
  providedIn: 'root',
})
export class LoadingStatus {
  private readonly easing = inject(Easing)
  private readonly ticker = new BehaviorSubject<number>(0)
  private readonly _progression = signal<number | null>(null)
  private readonly _easing = signal<ProgressionEase>(ProgressionEase.LINEAR)

  public readonly progression = computed(() => {
    const progression = this._progression()

    return progression === null ? progression : this.easing.ease(progression / 100) * 100
  })

  public readonly destroy$ = inject(DestroyRef)

  constructor() {
    this.ticker.pipe(
      switchMap((tick) => {
        if (tick === null) {
          return EMPTY
        }

        return interval(50).pipe(
          takeWhile(() => {
            const current = this._progression()
            return current !== null && current < 99.9
          }),
          tap(() => {
            this._progression.update((current) => {
              if (current === null) {
                throw new Error('Progression should not be null here')
              }

              // Slows down as it approaches 100%
              const increment = (100 - current) * 0.05 + 0.01
              return Math.min(99.9, current + increment)
            })
          }),
        )
      }),
    ).subscribe()

    this.destroy$.onDestroy(() => {
      this.ticker.complete()
    })
  }

  public start(ease: ProgressionEase = ProgressionEase.LINEAR) {
    this._easing.set(ease)
    this._progression.set(0)

    // Start the ticker
    this.ticker.next(0)
  }

  public setProgression(value: number) {
    const clampedValue = Math.max(0, Math.min(100, value))

    this._progression.set(clampedValue)
    this.stop()
  }

  public stop() {
    this._progression.set(100)

    setTimeout(() => {
      this._progression.set(null)
    }, 300)
  }
}
