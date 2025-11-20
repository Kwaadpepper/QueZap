import { computed, inject } from '@angular/core'

import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals'
import {
  catchError, concatMap, map, of, tap, throwError,
} from 'rxjs'

import { isFailure } from '@quezap/core/types'
import { AuthenticatedUser, AuthTokens } from '@quezap/domain/models'
import { AUTHENTICATION_SERVICE, TokenPersitance } from '@quezap/features/auth/services'

interface AuthenticatedUserState {
  _temporaryTokens?: AuthTokens
  _authenticated?: {
    user: AuthenticatedUser
    tokens: AuthTokens
  }
  authenticating: boolean
  sessionExpired: boolean
}

const initialState: AuthenticatedUserState = {
  _temporaryTokens: undefined,
  _authenticated: undefined,
  authenticating: false,
  sessionExpired: false,
}

export const AuthenticatedUserStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(store => ({
    isLoggedIn: computed(() => {
      return store._authenticated?.() !== undefined && !store.sessionExpired()
    }),
    currentTokens: computed(() => {
      return store._authenticated?.()?.tokens ?? store._temporaryTokens?.()
    }),
    currentUser: computed(() => {
      return store._authenticated?.()?.user
    }),
  })),

  withMethods((
    store,
    tokenPersistance = inject(TokenPersitance),
    authService = inject(AUTHENTICATION_SERVICE),
  ) => ({

    loadInitialState: () => {
      const tokens = tokenPersistance.getTokens()

      if (tokens === undefined) {
        return of(void 0)
      }

      patchState(store, {
        _temporaryTokens: tokens,
        authenticating: true,
      })

      return authService.me().pipe(
        concatMap((maybeUser) => {
          if (isFailure(maybeUser)) {
            tokenPersistance.removeTokens()
            patchState(store, initialState)
            patchState(store, { sessionExpired: true })
            return throwError(() => maybeUser.error)
          }

          const user = maybeUser.result

          patchState(store, {
            _authenticated: { user, tokens },
            _temporaryTokens: undefined,
            authenticating: false,
            sessionExpired: false,
          })

          return of(void 0)
        }),
        catchError((err) => {
          tokenPersistance.removeTokens()
          patchState(store, initialState)
          patchState(store, { sessionExpired: true })
          return throwError(() => err)
        }),
      )
    },

    login: (email: string, password: string) => {
      patchState(store, { authenticating: true })

      return of({ email, password }).pipe(
        concatMap(credentials => authService.login(credentials.email, credentials.password)),
        concatMap((maybeTokens) => {
          if (isFailure(maybeTokens)) {
            return throwError(() => maybeTokens.error)
          }

          const tokens = maybeTokens.result
          tokenPersistance.saveTokens(tokens)
          patchState(store, { _temporaryTokens: tokens })

          return authService.me().pipe(
            concatMap((maybeUser) => {
              if (isFailure(maybeUser)) {
                return throwError(() => maybeUser.error)
              }

              const user = maybeUser.result
              return of({ tokens, user })
            }),
          )
        }),

        tap(({ tokens, user }) => {
          patchState(store, {
            _authenticated: {
              user,
              tokens: tokens,
            },
            _temporaryTokens: undefined,
            authenticating: false,
            sessionExpired: false,
          })
        }),
        map(() => { return }),
        catchError((err) => {
          tokenPersistance.removeTokens()
          patchState(store, initialState)
          return throwError(() => err)
        }),
      )
    },

    logout: () => {
      return authService.logout().pipe(
        concatMap((maybeVoid) => {
          if (isFailure(maybeVoid)) {
            return throwError(() => maybeVoid.error)
          }

          console.log('Logout successful.')
          tokenPersistance.removeTokens()
          patchState(store, initialState)
          return of(undefined)
        }),
        catchError((err) => {
          console.error('Logout service failed, but forcing local state reset.', err)
          tokenPersistance.removeTokens()
          patchState(store, initialState)
          return throwError(() => err)
        }),
      )
    },

    refresh: () => {
      const currentAuth = store._authenticated?.()

      if (!currentAuth) {
        return throwError(() =>
          new Error('No authenticated user to refresh tokens for.'),
        )
      }

      return authService
        .refresh(currentAuth.tokens)
        .pipe(
          concatMap((maybeTokens) => {
            if (isFailure(maybeTokens)) {
              return throwError(() => maybeTokens.error)
            }

            const newTokens: AuthTokens = maybeTokens.result
            tokenPersistance.saveTokens(newTokens)
            patchState(store, {
              _authenticated: {
                ...currentAuth,
                tokens: newTokens,
              },
              sessionExpired: false,
            })

            return of(undefined)
          }),
          catchError((err) => {
            console.error('Token refresh failed:', err)
            tokenPersistance.removeTokens()
            patchState(store, initialState)
            patchState(store, { sessionExpired: true })
            return throwError(() => err)
          }),
        )
    },
  })),
)
