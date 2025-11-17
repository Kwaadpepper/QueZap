import { computed, inject, Injector, runInInjectionContext } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import { patchState, signalState, SignalState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, firstValueFrom, Observable, of, throwError } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { Participant, Session, SessionCode } from '@quezap/domain/models'

import { SESSION_API_SERVICE, SESSION_OBSERVER_SERVICE } from '../services'

import { ActiveSessionPersistence } from './../services/active-session-persistence/active-session-persistence'

interface ActiveSessionState {
  _session?: Session
  _nickname: SignalState<{
    value: string
    remembered: boolean
  }> | undefined
  _sessionIsLoaded: boolean
  _participants: Participant[]
}

const initialState: ActiveSessionState = {
  _session: undefined,
  _nickname: undefined,
  _sessionIsLoaded: false,
  _participants: [],
}

export const ActiveSessionStore = signalStore(
  withState(initialState),
  withComputed(store => ({
    session: computed(() => store._session?.()),
    nickname: computed(() => store._nickname?.()),
    restorationComplete: computed(() => store._sessionIsLoaded()),
    participants: computed(() => store._participants()),
  })),

  // Add current question as linked state
  // withLinkedState(({ options }) => ({
  //   selectedOption: linkedSignal<Option[], Option>({
  //     source: options,
  //     computation: (newOptions, previous) => {
  //       const option = newOptions.find((o) => o.id === previous?.value.id);
  //       return option ?? newOptions[0];
  //     },
  //   })
  // }))

  withMethods((
    store,
    sessionApi = inject(SESSION_API_SERVICE),
    sessionObserver = inject(SESSION_OBSERVER_SERVICE),
    activeSessionPersistence = inject(ActiveSessionPersistence),
  ) => ({

    startSession: (code: SessionCode): Observable<void> => {
      return sessionApi.find(code).pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            return throwError(() => response.error)
          }

          patchState(store, { _session: response.result })

          activeSessionPersistence.patch({ code })

          return of(void 0)
        }),
        catchError((err) => {
          patchState(store, initialState)

          return throwError(() => err)
        }),
      )
    },

    chooseNickname: (nickname: string, remember: boolean): Observable<void | ValidationError> => {
      return sessionApi.chooseNickname(nickname).pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            activeSessionPersistence.persistNickname(remember ? nickname : undefined)

            if (response.error instanceof ValidationError) {
              return of(response.error)
            }

            patchState(store, { _nickname: undefined })

            return throwError(() => response.error)
          }

          activeSessionPersistence.persistNickname(remember ? nickname : undefined)

          patchState(store, {
            _nickname: signalState({
              value: nickname,
              remembered: remember,
            }),
          })

          return of(void 0)
        }),
        catchError((err) => {
          patchState(store, { _nickname: undefined })

          activeSessionPersistence.persistNickname()

          return throwError(() => err)
        }),
      )
    },

    _loadParticipantsStream: (): Observable<Participant[]> => {
      return sessionObserver.participants().pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            return throwError(() => response.error)
          }

          return of(response.result)
        }),
        catchError((err) => {
          console.error('Erreur du flux des participants', err)
          return of([])
        }),
      )
    },
  })),

  withHooks({
    onInit(store, activeSessionPersistence = inject(ActiveSessionPersistence), injector = inject(Injector)) {
      const data = activeSessionPersistence.retrieve()
      if (data === null) {
        patchState(store, { _sessionIsLoaded: true })
        return
      }

      patchState(store, {
        ...initialState,
        _nickname: signalState({
          value: data.nickname ?? '',
          remembered: !!data.nickname,
        }),
      })

      firstValueFrom(
        store.startSession(data.code),
      ).then(() => {
        // In order to be destroyed with the store
        runInInjectionContext(injector, () => {
          store._loadParticipantsStream().pipe(
            // Prevent memory leaks if the store is destroyed
            takeUntilDestroyed(),
          ).subscribe((participants) => {
            patchState(store, { _participants: participants })
          })
        })
      }).catch(() => {
        activeSessionPersistence.clearKeepingNickname()
      }).finally(() => {
        patchState(store, { _sessionIsLoaded: true })
      })
    },
  }),
)
