import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { ForbidenError, UnauthorizedError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceOutput } from '@quezap/core/types'
import { AuthenticatedUser, UserId } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'
import { JWT } from '@quezap/domain/types'

import { AuthenticationService } from './authentication'

interface LoginApiResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: {
    id: string
    name: string
  }
}

@Injectable({ providedIn: 'root' })
export class AuthenticationHttpService implements AuthenticationService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  login(identifier: string, password: string): ServiceOutput<AuthTokens, UnauthorizedError> {
    return this.http.post<LoginApiResponse>(`${this.apiUrl}/auth/login`, { identifier, password }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          accessToken: res.accessToken as JWT,
          refreshToken: res.refreshToken as JWT,
        },
      })),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 400) {
          return of({
            kind: 'failure' as const,
            error: new UnauthorizedError('Identifiants invalides'),
          })
        }
        return of({
          kind: 'failure' as const,
          error: new UnauthorizedError(error.message || 'Erreur d\'authentification'),
        })
      }),
    )
  }

  refresh(tokens: AuthTokens): ServiceOutput<AuthTokens> {
    return this.http.post<LoginApiResponse>(`${this.apiUrl}/auth/refresh`, { refreshToken: tokens.refreshToken }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          accessToken: res.accessToken as JWT,
          refreshToken: res.refreshToken as JWT,
        },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new UnauthorizedError(error.message || 'Session expirée'),
      })),
    )
  }

  logout(): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/logout`, {}).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(() => of({ kind: 'success' as const, result: undefined })),
    )
  }

  me(): ServiceOutput<AuthenticatedUser, ForbidenError> {
    return this.http.get<{ id: string, name: string }>(`${this.apiUrl}/auth/me`).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          id: res.id as UserId,
          pseudo: res.name,
        },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ForbidenError(error.message || 'Non autorisé'),
      })),
    )
  }

  askToResetPassword(email: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/forgotten-password`, { email }).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({}, error.message || 'Erreur lors de la demande'),
      })),
    )
  }

  verifyResetToken(token: string): ServiceOutput<void, ForbidenError> {
    return this.http.get<void>(`${this.apiUrl}/auth/verify-reset-token`, { params: { token } }).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ForbidenError(error.message || 'Jeton de réinitialisation invalide ou expiré'),
      })),
    )
  }

  resetPassword(token: string, newPassword: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/auth/reset-password`, { token, newPassword }).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({}, error.message || 'Erreur lors de la réinitialisation'),
      })),
    )
  }
}
