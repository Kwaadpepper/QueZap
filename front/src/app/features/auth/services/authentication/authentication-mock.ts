import { Observable, Subject } from 'rxjs'

import { zod, zodToExternalValidationError } from '@quezap/core/tools'
import { AuthenticatedUser } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'

import { AuthenticationService } from './authentication'

const validationLoginSchema = zod.object({
  email: zod.email().refine(v => v === 'user@example.net'),
  password: zod.string().refine(v => v === 'password'),
})
const validationRefreshSchema = zod.jwt()
const validationAskResetPasswordSchema = zod.object({
  email: zod.email().refine(v => v === 'user@example.net'),
})

export class AuthenticationMockService implements AuthenticationService {
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)
  private readonly MOCKED_USER: AuthenticatedUser = {
    uuid: '123e4567-e89b-12d3-a456-426614174000',
    pseudo: 'Jane Doe',
  }

  login(email: string, password: string): Observable<AuthTokens> {
    const response = new Subject<AuthTokens>()

    validationLoginSchema
      .safeParseAsync({ email, password })
      .then((result) => {
        setTimeout(() => {
          if (!result.success) {
            response.error(
              zodToExternalValidationError(result.error),
            )
            return
          }

          if (Math.random() < 0.2) {
            response.error(new Error('Login failed due to network error'))
            return
          }

          response.next({
            accessToken: this.randomJwtToken(),
            refreshToken: this.randomJwtToken(),
          })
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }

  refresh(tokens: AuthTokens): Observable<AuthTokens> {
    const response = new Subject<AuthTokens>()

    validationRefreshSchema
      .safeParseAsync(this.randomJwtToken())
      .then((result) => {
        setTimeout(() => {
          if (!result.success) {
            response.error(
              zodToExternalValidationError(result.error),
            )
            return
          }

          if (Math.random() < 0.2) {
            response.error(new Error('Login failed due to network error'))
            return
          }

          response.next({
            accessToken: this.randomJwtToken(),
            refreshToken: tokens.refreshToken,
          })
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }

  logout(): Observable<void> {
    const response = new Subject<void>()

    setTimeout(() => {
      if (Math.random() < 0.2) {
        response.error(new Error('Logout failed due to network error'))
        return
      }

      response.next()
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  me(): Observable<AuthenticatedUser> {
    const response = new Subject<AuthenticatedUser>()
    setTimeout(() => {
      // Simulate a 20% chance of network error
      if (Math.random() < 0.2) {
        response.error(new Error('Failed to fetch user info due to network error'))
        return
      }
      response.next({ ...this.MOCKED_USER })
      response.complete()
    }, this.MOCK_DELAY())
    return response
  }

  resetPassword(email: string): Observable<void> {
    const response = new Subject<void>()

    validationAskResetPasswordSchema
      .safeParseAsync({ email })
      .then((result) => {
        setTimeout(() => {
          if (!result.success) {
            response.error(
              zodToExternalValidationError(result.error),
            )
            return
          }

          if (Math.random() < 0.2) {
            response.error(new Error('Ask to reset failed due to network error'))
            return
          }

          response.next()
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }

  private randomJwtToken(): string {
    const header = {
      alg: 'HS256',
      typ: 'JWT',
    }

    const now = Math.floor(Date.now() / 1000)
    const payload = {
      sub: this.MOCKED_USER.uuid,
      iat: now,
      exp: now + (60 * 60),
    }

    const base64UrlEncode = (obj: unknown): string => {
      const json = JSON.stringify(obj)

      return btoa(json)
        .replaceAll('+', '-')
        .replaceAll('/', '_')
        .replace(/=+$/, '')
    }

    const encodedHeader = base64UrlEncode(header)
    const encodedPayload = base64UrlEncode(payload)

    const randomBytes = new Uint8Array(32)
    crypto.getRandomValues(randomBytes)
    const fakeSignature = btoa(String.fromCodePoint(...randomBytes))
      .replaceAll('+', '-')
      .replaceAll('/', '_')
      .replace(/=+$/, '')

    return `${encodedHeader}.${encodedPayload}.${fakeSignature}`
  }
}
