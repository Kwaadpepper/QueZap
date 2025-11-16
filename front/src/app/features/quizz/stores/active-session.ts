import { computed, inject } from '@angular/core'

import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, Observable, of, throwError } from 'rxjs'

import { isFailure } from '@quezap/core/types'
import { Session, SessionCode } from '@quezap/domain/models'

import { SESSION_SERVICE } from '../services'

interface ActiveSessionState {
  _session?: Session
}

const initialState: ActiveSessionState = {
  _session: undefined,
}

export const ActiveSessionStore = signalStore(
  withState(initialState),
  withComputed(store => ({
    session: computed(() => {
      return store._session?.()
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

  withHooks({
    // onDestroy(store) {
    // Notify service that session is no longer active
    // },
  }),

  withMethods((
    store,
    sessionService = inject(SESSION_SERVICE),
  ) => ({

    startSession: (code: SessionCode): Observable<void> => {
      return sessionService.find(code).pipe(
        concatMap((session) => {
          if (isFailure(session)) {
            return throwError(() => session.error)
          }

          patchState(store, { _session: session.result })

          return of(void 0)
        }),
        catchError((err) => {
          patchState(store, initialState)

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
)
