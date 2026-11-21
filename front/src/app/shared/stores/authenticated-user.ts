import { computed, inject } from '@angular/core'

import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals'
import {
  catchError, concatMap, map, of, tap, throwError,
} from 'rxjs'

import { isFailure } from '@quezap/core/types'
import { AuthenticatedUser, AuthTokens } from '@quezap/domain/models'
import { AUTHENTICATION_SERVICE, TokenPersitance } from '@quezap/features/auth/services'

const SESSION_HINT_KEY = 'quezap_has_session'

function hasSessionHint(): boolean {
  try {
    return typeof localStorage !== 'undefined' && localStorage.getItem(SESSION_HINT_KEY) === '1'
  }
  catch {
    return false
  }
}

function setSessionHint(active: boolean): void {
  try {
    if (typeof localStorage === 'undefined') {
      return
    }
    if (active) {
      localStorage.setItem(SESSION_HINT_KEY, '1')
    }
    else {
      localStorage.removeItem(SESSION_HINT_KEY)
    }
  }
  catch {
    // Ignore storage errors (e.g. private browsing restrictions)
  }
}

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
      const existingTokens = tokenPersistance.getTokens()

      if (existingTokens !== undefined) {
        patchState(store, {
          _temporaryTokens: existingTokens,
          authenticating: true,
        })

        return authService.me().pipe(
          concatMap((maybeUser) => {
            if (isFailure(maybeUser)) {
              tokenPersistance.removeTokens()
              patchState(store, { ...initialState, sessionExpired: true })
              return of(void 0)
            }

            const user = maybeUser.result
            const latestTokens = store.currentTokens() ?? tokenPersistance.getTokens() ?? existingTokens

            patchState(store, {
              _authenticated: { user, tokens: latestTokens },
              _temporaryTokens: undefined,
              authenticating: false,
              sessionExpired: false,
            })

            return of(void 0)
          }),
          catchError(() => {
            tokenPersistance.removeTokens()
            patchState(store, { ...initialState, sessionExpired: true })
            return of(void 0)
          }),
        )
      }

      if (!hasSessionHint()) {
        return of(void 0)
      }

      patchState(store, { authenticating: true })

      return authService.refresh().pipe(
        concatMap((maybeTokens) => {
          if (isFailure(maybeTokens)) {
            tokenPersistance.removeTokens()
            setSessionHint(false)
            patchState(store, initialState)
            return of(void 0)
          }

          const refreshedTokens = maybeTokens.result
          tokenPersistance.saveTokens(refreshedTokens)
          patchState(store, { _temporaryTokens: refreshedTokens })

          return authService.me().pipe(
            concatMap((maybeUser) => {
              if (isFailure(maybeUser)) {
                tokenPersistance.removeTokens()
                setSessionHint(false)
                patchState(store, initialState)
                return of(void 0)
              }

              const user = maybeUser.result
              const latestTokens = store.currentTokens() ?? tokenPersistance.getTokens() ?? refreshedTokens

              patchState(store, {
                _authenticated: { user, tokens: latestTokens },
                _temporaryTokens: undefined,
                authenticating: false,
                sessionExpired: false,
              })

              return of(void 0)
            }),
          )
        }),
        catchError(() => {
          tokenPersistance.removeTokens()
          setSessionHint(false)
          patchState(store, initialState)
          return of(void 0)
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
          setSessionHint(true)
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
          setSessionHint(false)
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
          setSessionHint(false)
          patchState(store, initialState)
          return of(undefined)
        }),
        catchError((err) => {
          console.error('Logout service failed, but forcing local state reset.', err)
          tokenPersistance.removeTokens()
          setSessionHint(false)
          patchState(store, initialState)
          return throwError(() => err)
        }),
      )
    },

    refresh: () => {
      const currentAuth = store._authenticated?.()
      const temporaryTokens = store._temporaryTokens?.()
      const tokensToRefresh = currentAuth?.tokens ?? temporaryTokens

      if (!tokensToRefresh) {
        return throwError(() =>
          new Error('No authenticated user to refresh tokens for.'),
        )
      }

      return authService
        .refresh()
        .pipe(
          concatMap((maybeTokens) => {
            if (isFailure(maybeTokens)) {
              return throwError(() => maybeTokens.error)
            }

            const newTokens: AuthTokens = maybeTokens.result
            tokenPersistance.saveTokens(newTokens)

            if (currentAuth) {
              patchState(store, {
                _authenticated: {
                  ...currentAuth,
                  tokens: newTokens,
                },
                sessionExpired: false,
              })
            }
            else {
              patchState(store, {
                _temporaryTokens: newTokens,
                sessionExpired: false,
              })
            }

            return of(undefined)
          }),
          catchError((err) => {
            console.error('Token refresh failed:', err)
            tokenPersistance.removeTokens()
            setSessionHint(false)
            patchState(store, { ...initialState, sessionExpired: true })
            return throwError(() => err)
          }),
        )
    },
  })),
)
