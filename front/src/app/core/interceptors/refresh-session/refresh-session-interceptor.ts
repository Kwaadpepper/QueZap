import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http'
import { inject } from '@angular/core'

import {
  catchError, defer, finalize, Observable, retry, shareReplay, switchMap, throwError,
} from 'rxjs'

import { IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { AuthTokens } from '@quezap/domain/models'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

let refreshInFlight$: Observable<void> | null = null

function cloneRequestWithAuthHeader(req: HttpRequest<unknown>, tokens: AuthTokens) {
  return req.clone({ setHeaders: { Authorization: `Bearer ${tokens.accessToken}` } })
}

export const refreshSessionInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthenticatedUserStore)
  const requestTokens = store.currentTokens()

  if (requestTokens?.accessToken && !req.context.get(IS_REFRESHING_ACCESS_TOKEN)) {
    req = cloneRequestWithAuthHeader(req, requestTokens)
  }

  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse
        // ? Is this a 401 Unauthorized error?
        && error.status === 401
        // ? Are we able to refresh?
        && requestTokens
        && !req.context.get(IS_REFRESHING_ACCESS_TOKEN)
      ) {
        const latestTokens = store.currentTokens()
        if (latestTokens && latestTokens.accessToken !== requestTokens.accessToken) {
          return next(cloneRequestWithAuthHeader(req, latestTokens))
        }

        if (!refreshInFlight$) {
          refreshInFlight$ = defer(() => store.refresh()).pipe(
            // Retry once if refresh fails
            retry(1),
            finalize(() => {
              refreshInFlight$ = null
            }),
            shareReplay({ bufferSize: 1, refCount: true }),
          )
        }

        return refreshInFlight$.pipe(
          switchMap(() => {
            const refreshedTokens = store.currentTokens()

            if (!refreshedTokens) {
              return throwError(() => error)
            }

            return next(
              // Replay with new auth header
              cloneRequestWithAuthHeader(req, refreshedTokens),
            )
          }),
          catchError((refreshError) => {
            // Rethrow if refresh fails
            return throwError(() => refreshError)
          }),
        )
      }

      return throwError(() => error)
    }),
  )
}
