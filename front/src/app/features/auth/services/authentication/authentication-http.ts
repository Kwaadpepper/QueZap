import { inject, Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { LoginResponseDto, UserSummaryDto } from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { ForbidenError, UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'
import { AuthenticatedUser, UserId } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'
import { JWT } from '@quezap/domain/types'

import { TokenPersitance } from '../token-persistance/token-persitance'

import { AuthenticationService } from './authentication'

@Injectable({ providedIn: 'root' })
export class AuthenticationHttpService implements AuthenticationService {
  private readonly tokenPersitance = inject(TokenPersitance)
  login(identifier: string, password: string): ServiceOutput<AuthTokens, UnauthorizedError> {
    return from(
      client.post<{ 200: LoginResponseDto }>({
        url: '/apiv1/auth/login',
        body: { identifier, password },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.accessToken || !data?.refreshToken) {
          return {
            kind: 'failure' as const,
            error: new UnauthorizedError('Identifiants invalides'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            accessToken: data.accessToken as JWT,
            refreshToken: data.refreshToken as JWT,
          },
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

  refresh(tokens: AuthTokens): ServiceOutput<AuthTokens> {
    return from(
      client.post<{ 200: LoginResponseDto }>({
        url: '/apiv1/auth/refresh',
        body: { refreshToken: tokens.refreshToken },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data?.accessToken || !data?.refreshToken) {
          return {
            kind: 'failure' as const,
            error: new UnauthorizedError('Session expirée'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            accessToken: data.accessToken as JWT,
            refreshToken: data.refreshToken as JWT,
          },
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
    )
  }

  logout(refreshToken?: JWT): ServiceOutput<void> {
    const token = refreshToken ?? this.tokenPersitance.getTokens()?.refreshToken
    if (!token) {
      return of({ kind: 'success' as const, result: undefined })
    }

    return from(
      client.post<void>({
        url: '/apiv1/auth/logout',
        body: { refreshToken: token },
      }),
    ).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(() => of({ kind: 'success' as const, result: undefined })),
    )
  }

  me(): ServiceOutput<AuthenticatedUser, ForbidenError> {
    return from(
      client.get<{ 200: UserSummaryDto }>({ url: '/apiv1/auth/me' }),
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

  askToResetPassword(email: string): ServiceOutput<void> {
    return from(
      client.post<void>({
        url: '/apiv1/auth/forgotten-password',
        body: { email },
      }),
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
    return from(
      client.get<void>({
        url: '/apiv1/auth/verify-reset-token',
        query: { token },
      }),
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
    return from(
      client.post<void>({
        url: '/apiv1/auth/reset-password',
        body: { token, newPassword },
      }),
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
