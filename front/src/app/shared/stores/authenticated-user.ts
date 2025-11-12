import { computed, inject } from '@angular/core'

import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, EMPTY, map, of, tap, throwError } from 'rxjs'

import { AUTHENTICATION_SERVICE } from '@quezap/auth/services'
import { AuthenticatedUser, AuthTokens } from '@quezap/domain/models'

interface AuthenticatedUserState {
  _authenticated?: {
    user: AuthenticatedUser
    tokens: AuthTokens
  }
  authenticating: boolean
  sessionExpired: boolean
}

const initialState: AuthenticatedUserState = {
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
      return store._authenticated?.()?.tokens
    }),
    currentUser: computed(() => {
      return store._authenticated?.()?.user
    }),
  })),

  withMethods((store, authService = inject(AUTHENTICATION_SERVICE)) => ({

    login: (email: string, password: string) => {
      patchState(store, { authenticating: true })

      return of({ email, password }).pipe(
        concatMap(credentials => authService.login(credentials.email, credentials.password)),
        concatMap(tokens => authService.me().pipe(
          map(user => ({
            tokens,
            user,
          })),
        )),
        tap(({ tokens, user }) => {
          patchState(store, {
            _authenticated: {
              user,
              tokens: tokens,
            },
            authenticating: false,
            sessionExpired: false,
          })
        }),
        map(() => { return }),
        catchError((err) => {
          patchState(store, {
            _authenticated: undefined,
            authenticating: false,
            sessionExpired: false,
          })

          return throwError(() => err)
        }),
      )
    },

    logout: () => {
      return authService.logout().pipe(
        tap(() => {
          patchState(store, initialState)
        }),
        catchError((err) => {
          console.error('Logout service failed, but forcing local state reset.', err)
          patchState(store, initialState)

          return EMPTY
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
          tap((newTokens: AuthTokens) => {
            patchState(store, {
              _authenticated: {
                ...currentAuth,
                tokens: newTokens,
              },
              sessionExpired: false,
            })
          }),
          catchError((err) => {
            patchState(store, {
              _authenticated: undefined,
              sessionExpired: true,
              authenticating: false,
            })
            return throwError(() => err)
          }),
        )
    },
  })),
)
