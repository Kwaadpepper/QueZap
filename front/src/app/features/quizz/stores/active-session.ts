import { computed, ErrorHandler, inject, Injector, runInInjectionContext } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import { patchState, signalState, SignalState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, firstValueFrom, Observable, of, retry, throwError } from 'rxjs'

import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { Participant, Session, SessionCode, sessionIsRunning } from '@quezap/domain/models'

import { SESSION_API_SERVICE, SESSION_OBSERVER_SERVICE, sessionEnded, sessionStarted } from '../services'

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
    sessionIsRunning: computed(() => {
      const session = store._session?.()
      return session ? sessionIsRunning(session) : false
    }),
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
    errorHandler = inject(ErrorHandler),
  ) => ({

    joinSession: (code: SessionCode): Observable<void | NotFoundError> => {
      return sessionApi.find(code).pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            patchState(store, initialState)

            const error = response.error
            if (error instanceof NotFoundError) {
              return of(error)
            }

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

    _listenSessionStatus: (): Observable<void> => {
      return sessionObserver.sessionEvents().pipe(
        // retry(5),
        concatMap((response) => {
          if (isFailure(response)) {
            return throwError(() => response.error)
          }

          const session = store.session()
          const event = response.result

          if (!session) {
            return of(void 0)
          }

          if (sessionStarted(event)) {
            patchState(store, {
              _session: {
                ...session,
                startedAt: event.session.startedAt,
              },
            })
          }

          if (sessionEnded(event)) {
            patchState(store, {
              _session: {
                ...session,
                endedAt: event.session.endedAt,
              },
            })
          }

          return of(void 0)
        }),
      )
    },

    _loadParticipantsStream: (): Observable<Participant[]> => {
      return sessionObserver.participants().pipe(
        // retry(5),
        concatMap((response) => {
          if (isFailure(response)) {
            return throwError(() => response.error)
          }

          return of(response.result)
        }),
        catchError((err) => {
          console.error('Erreur du flux des participants', err)
          errorHandler.handleError(err)
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

      // ! FIXME: Error in there should be handled somehow else than just blocking the app
      firstValueFrom(
        store.joinSession(data.code).pipe(retry(1)),
      ).then(() => {
        // In order to be destroyed with the store
        runInInjectionContext(injector, () => {
          // Listen to session status updates
          store._loadParticipantsStream().pipe(
            // Prevent memory leaks if the store is destroyed
            takeUntilDestroyed(),
          ).subscribe((participants) => {
            patchState(store, { _participants: participants })
          })

          // Listen to session status updates
          store._listenSessionStatus().pipe(
            // Prevent memory leaks if the store is destroyed
            takeUntilDestroyed(),
          ).subscribe()
        })
      }).catch(() => {
        activeSessionPersistence.clearKeepingNickname()
      }).finally(() => {
        patchState(store, { _sessionIsLoaded: true })
      })
    },
  }),
)
