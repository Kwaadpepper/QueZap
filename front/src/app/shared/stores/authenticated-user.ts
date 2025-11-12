import { computed, inject } from '@angular/core'

import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals'
import { catchError, concatMap, EMPTY, map, of, tap, throwError } from 'rxjs'

import { AUTHENTICATION_SERVICE, TokenPersitance } from '@quezap/auth/services'
import { AuthenticatedUser, AuthTokens } from '@quezap/domain/models'

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
        return EMPTY
      }

      patchState(store, {
        _temporaryTokens: tokens,
        authenticating: true,
      })

      return authService.me().pipe(
        tap((user) => {
          patchState(store, {
            _authenticated: { user, tokens },
            _temporaryTokens: undefined,
            authenticating: false,
            sessionExpired: false,
          })
        }),
        map(() => { return }),
        catchError(() => {
          tokenPersistance.removeTokens()
          patchState(store, initialState)
          patchState(store, { sessionExpired: true })
          return EMPTY
        }),
      )
    },

    login: (email: string, password: string) => {
      patchState(store, { authenticating: true })

      return of({ email, password }).pipe(
        concatMap(credentials => authService.login(credentials.email, credentials.password)),
        tap((tokens) => {
          tokenPersistance.saveTokens(tokens)
          patchState(store, { _temporaryTokens: tokens })
        }),
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
        tap(() => {
          tokenPersistance.removeTokens()
          patchState(store, initialState)
        }),
        catchError((err) => {
          console.error('Logout service failed, but forcing local state reset.', err)
          tokenPersistance.removeTokens()
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
            tokenPersistance.saveTokens(newTokens)
            patchState(store, {
              _authenticated: {
                ...currentAuth,
                tokens: newTokens,
              },
              sessionExpired: false,
            })
          }),
          catchError((err) => {
            tokenPersistance.removeTokens()
            patchState(store, initialState)
            patchState(store, { sessionExpired: true })
            return throwError(() => err)
          }),
        )
    },
  })),
)
