import { computed, inject } from '@angular/core'

import { patchState, signalState, SignalState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, firstValueFrom, Observable, of, throwError } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

import { SESSION_SERVICE } from '../services'

import { ActiveSessionPersistence } from './../services/active-session-persistence/active-session-persistence'

interface ActiveSessionState {
  _session?: Session
  _nickname: SignalState<{
    value: string
    remembered: boolean
  }> | undefined
  _sessionIsLoaded: boolean
}

const initialState: ActiveSessionState = {
  _session: undefined,
  _nickname: undefined,
  _sessionIsLoaded: false,
}

export const ActiveSessionStore = signalStore(
  withState(initialState),
  withComputed(store => ({
    session: computed(() => store._session?.()),
    nickname: computed(() => store._nickname?.()),
    restorationComplete: computed(() => store._sessionIsLoaded()),
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
    sessionService = inject(SESSION_SERVICE),
    activeSessionPersistence = inject(ActiveSessionPersistence),
  ) => ({

    startSession: (code: SessionCode): Observable<void> => {
      return sessionService.find(code).pipe(
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
      return sessionService.chooseNickname(nickname).pipe(
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

    // login: (email: string, password: string) => {
    //   patchState(store, { authenticating: true })

    //   return of({ email, password }).pipe(
    //     concatMap(credentials => authService.login(credentials.email, credentials.password)),
    //     concatMap((maybeTokens) => {
    //       if (isFailure(maybeTokens)) {
    //         return throwError(() => maybeTokens.error)
    //       }

    //       const tokens = maybeTokens.result
    //       tokenPersistance.saveTokens(tokens)
    //       patchState(store, { _temporaryTokens: tokens })

    //       return authService.me().pipe(
    //         concatMap((maybeUser) => {
    //           if (isFailure(maybeUser)) {
    //             return throwError(() => maybeUser.error)
    //           }

    //           const user = maybeUser.result
    //           return of({ tokens, user })
    //         }),
    //       )
    //     }),

    //     tap(({ tokens, user }) => {
    //       patchState(store, {
    //         _authenticated: {
    //           user,
    //           tokens: tokens,
    //         },
    //         _temporaryTokens: undefined,
    //         authenticating: false,
    //         sessionExpired: false,
    //       })
    //     }),
    //     map(() => { return }),
    //     catchError((err) => {
    //       tokenPersistance.removeTokens()
    //       patchState(store, initialState)
    //       return throwError(() => err)
    //     }),
    //   )
    // },

  })),

  withHooks({
    onInit(store, activeSessionPersistence = inject(ActiveSessionPersistence)) {
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
      ).catch(() => {
        activeSessionPersistence.clearKeepingNickname()
      }).finally(() => {
        patchState(store, { _sessionIsLoaded: true })
      })
    },
  }),
)
