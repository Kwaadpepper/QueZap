import { computed, DestroyRef, inject, linkedSignal } from '@angular/core'

import {
  patchState, signalStore,
  withComputed,
  withHooks,
  withLinkedState, withMethods, withState,
} from '@ngrx/signals'

import { Timer } from '@quezap/domain/models'

interface TimerState {
  timer: Timer | undefined
}

const initialState: TimerState = { timer: undefined }

export const TimerStore = signalStore(
  withState(initialState),

  withComputed(store => ({
    started: computed(() => {
      const timer = store.timer()
      return timer !== undefined && timer.seconds > 0
    }),
  })),

  withLinkedState(({ timer }) => ({
    timeLeft: linkedSignal<number | undefined, number | undefined>({
      source: () => timer?.()?.seconds,
      computation: (newOptions, _) => {
        return newOptions
      },
    }),
  })),

  withMethods((store) => {
    let pendingTimeout: ReturnType<typeof setTimeout> | null = null

    const cancelPendingTimeout = () => {
      if (pendingTimeout !== null) {
        clearTimeout(pendingTimeout)
        pendingTimeout = null
      }
    }

    return {
      setTimer: (timer?: Timer) => {
        cancelPendingTimeout()
        patchState(store, { timer: undefined })
        // This is needed to ensure that the computed signals depending on timer are updated correctly
        pendingTimeout = setTimeout(() => {
          pendingTimeout = null
          patchState(store, {
            timer,
            timeLeft: timer?.seconds,
          })
        }, 0)
      },
      setTimeLeft: (timeLeft: number | undefined) => {
        patchState(store, { timeLeft })
      },
      clearTimer: () => {
        cancelPendingTimeout()
        patchState(store, { timer: undefined, timeLeft: undefined })
      },
    }
  }),

  withHooks({
    onInit(store, destroyRef = inject(DestroyRef)) {
      destroyRef.onDestroy(() => {
        store.clearTimer()
      })
    },
  }),
)
