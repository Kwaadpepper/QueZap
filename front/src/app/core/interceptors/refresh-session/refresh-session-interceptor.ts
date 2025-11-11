import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http'
import { inject } from '@angular/core'

import { catchError, retry, switchMap, throwError } from 'rxjs'

import { IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { AuthTokens } from '@quezap/domain/models'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

function cloneRequestWithAuthHeader(req: HttpRequest<unknown>, tokens: AuthTokens) {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${tokens.accessToken}`,
    },
  })
}

export const refreshSessionInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthenticatedUserStore)
  const currentTokens = store.currentTokens()

  if (currentTokens?.accessToken) {
    req = cloneRequestWithAuthHeader(req, currentTokens)
  }

  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse
        // ? Is this a 401 Unauthorized error?
        && error.status === 401
        // ? Are we able to refresh?
        && currentTokens
        && !req.context.get(IS_REFRESHING_ACCESS_TOKEN)
      ) {
        // Pour éviter une boucle infinie de rafraîchissement, nous vérifions
        // généralement si la requête elle-même n'est pas déjà celle de rafraîchissement.
        // Ici, nous supposons que le service d'authentification gère cela,
        // mais une vérification d'URL est souvent nécessaire en production.

        // Utiliser switchMap pour passer de l'erreur à l'Observable de rafraîchissement
        return store.refresh().pipe(
          // Retry once if refresh fails
          retry(1),
          switchMap((newTokens) => {
            return next(
              // Replay with new auth header
              cloneRequestWithAuthHeader(req, newTokens),
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
