import { HttpClient, HttpContext } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import {
  catchError, defer, finalize, from, map, of, shareReplay,
} from 'rxjs'

import { type Options, RefreshData } from '@quezap/core/api-client'
import {
  forgottenPassword, login, logout, me, refresh, resetPassword, verifyResetToken,
} from '@quezap/core/api-client/sdk.gen'
import { ForbidenError, UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { ServiceOutput } from '@quezap/core/types'
import { AuthenticatedUser, UserId } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'
import { JWT } from '@quezap/domain/types'

import { AuthenticationService } from './authentication'

@Injectable({ providedIn: 'root' })
export class AuthenticationHttpService implements AuthenticationService {
  private readonly httpClient = inject(HttpClient, { optional: true })
  private refreshInFlight$: ServiceOutput<AuthTokens> | null = null

  login(identifier: string, password: string): ServiceOutput<AuthTokens, UnauthorizedError> {
    return defer(() =>
      from(
        login({
          body: { identifier, password },
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.accessToken) {
          return {
            kind: 'failure' as const,
            error: new UnauthorizedError('Identifiants invalides'),
          }
        }

        return {
          kind: 'success' as const,
          result: { accessToken: data.accessToken as JWT },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new UnauthorizedError(
            error instanceof Error ? error.message : 'Erreur d\'authentification',
          ),
        }),
      ),
    )
  }

  refresh(): ServiceOutput<AuthTokens> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$
    }

    const refreshOptions: Options<RefreshData, false> & { context: HttpContext } = {
      context: new HttpContext().set(IS_REFRESHING_ACCESS_TOKEN, true),
      ...(this.httpClient ? { httpClient: this.httpClient } : {}),
    }

    this.refreshInFlight$ = defer(() => from(refresh(refreshOptions))).pipe(
      map(({ data, error }) => {
        if (error || !data?.accessToken) {
          return {
            kind: 'failure' as const,
            error: new UnauthorizedError('Session expirée'),
          }
        }

        return {
          kind: 'success' as const,
          result: { accessToken: data.accessToken as JWT },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new UnauthorizedError(
            error instanceof Error ? error.message : 'Session expirée',
          ),
        }),
      ),
      finalize(() => {
        this.refreshInFlight$ = null
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
    )

    return this.refreshInFlight$
  }

  logout(): ServiceOutput<void> {
    return defer(() =>
      from(
        logout(this.httpClient ? { httpClient: this.httpClient } : undefined),
      ),
    ).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(() => of({ kind: 'success' as const, result: undefined })),
    )
  }

  me(): ServiceOutput<AuthenticatedUser, ForbidenError> {
    return defer(() =>
      from(me(this.httpClient ? { httpClient: this.httpClient } : undefined)),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.id) {
          return {
            kind: 'failure' as const,
            error: new ForbidenError('Non autorisé'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            id: data.id as UserId,
            pseudo: data.name ?? '',
          },
        }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ForbidenError(
            error instanceof Error ? error.message : 'Non autorisé',
          ),
        }),
      ),
    )
  }

  askToResetPassword(email: string, turnstileToken?: string): ServiceOutput<void> {
    return defer(() =>
      from(
        forgottenPassword({
          body: {
            identifier: email,
            ...(turnstileToken ? { turnstileToken } : {}),
          },
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de la demande'),
          }
        }

        return { kind: 'success' as const, result: undefined }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            error instanceof Error ? error.message : 'Erreur lors de la demande',
          ),
        }),
      ),
    )
  }

  verifyResetToken(token: string): ServiceOutput<void, ForbidenError> {
    return defer(() =>
      from(
        verifyResetToken({
          body: { token },
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ForbidenError('Jeton de réinitialisation invalide ou expiré'),
          }
        }

        return { kind: 'success' as const, result: undefined }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ForbidenError(
            error instanceof Error
              ? error.message
              : 'Jeton de réinitialisation invalide ou expiré',
          ),
        }),
      ),
    )
  }

  resetPassword(token: string, newPassword: string): ServiceOutput<void> {
    return defer(() =>
      from(
        resetPassword({
          body: { token, newPassword },
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de la réinitialisation'),
          }
        }

        return { kind: 'success' as const, result: undefined }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            error instanceof Error ? error.message : 'Erreur lors de la réinitialisation',
          ),
        }),
      ),
    )
  }
}
