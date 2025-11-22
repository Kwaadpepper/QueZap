import { computed, linkedSignal } from '@angular/core'

import {
  patchState, signalStore,
  withComputed,
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

  withMethods(store => ({
    setTimer: (timer?: Timer) => {
      patchState(store, { timer: undefined })
      // This is needed to ensure that the computed signals depending on timer are updated correctly
      setTimeout(() => {
        patchState(store, {
          timer,
          timeLeft: timer?.seconds,
        })
      }, 0)
    },
    setTimeLeft: (timeLeft: number | undefined) => {
      patchState(store, { timeLeft })
    },
  })),
)
