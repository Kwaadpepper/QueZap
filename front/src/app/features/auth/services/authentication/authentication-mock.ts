import { HttpErrorResponse } from '@angular/common/http'
import { inject } from '@angular/core'

import { Subject } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { UnauthorizedError } from '@quezap/core/errors/unauthorized-error'
import { zod } from '@quezap/core/tools/zod'
import { zodToExternalValidationError } from '@quezap/core/tools/zod-to-external-validation-error'
import { ServiceOutput, Tried } from '@quezap/core/types'
import { AuthenticatedUser, UserId } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'
import { JWT } from '@quezap/domain/types'
import { TokenPersitance } from '@quezap/features/auth/services'

import { AuthenticationService } from './authentication'

const validationLoginSchema = zod.object({
  email: zod.email().refine(v => v === 'user@example.net'),
  password: zod.string().refine(v => v === 'password'),
})
const validationRefreshSchema = zod.jwt()
const validationAskResetPasswordSchema = zod.object({ email: zod.email().refine(v => v === 'user@example.net') })

const validationVerifyResetTokenSchema = zod.jwt()

const validationResetPasswordSchema = zod.object({
  token: zod.jwt(),
  newPassword: zod.string().min(8),
})

export class AuthenticationMockService implements AuthenticationService {
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)
  private readonly MOCKED_USER: AuthenticatedUser = {
    id: '123e4567-e89b-12d3-a456-426614174000' as UserId,
    pseudo: 'Jane Doe',
  }

  private readonly tokenPersitance = inject(TokenPersitance)

  login(email: string, password: string): ServiceOutput<AuthTokens> {
    const response = new Subject<Tried<AuthTokens>>()

    setTimeout(() => {
      const result = validationLoginSchema.safeParse({ email, password })
      if (!result.success) {
        response.next(zodToExternalValidationError(result.error))
        response.complete()
        return
      }

      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        return
      }

      response.next({
        kind: 'success',
        result: {
          accessToken: this.randomJwtToken(),
          refreshToken: this.randomJwtToken(),
        },
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  refresh(tokens: AuthTokens): ServiceOutput<AuthTokens> {
    const response = new Subject<Tried<AuthTokens>>()

    setTimeout(() => {
      const result = validationRefreshSchema.safeParse(tokens.refreshToken)
      if (!result.success) {
        response.next(zodToExternalValidationError(result.error))
        response.complete()
        return
      }

      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        return
      }

      response.next({
        kind: 'success',
        result: {
          accessToken: this.randomJwtToken(),
          refreshToken: tokens.refreshToken,
        },
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  logout(): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    setTimeout(() => {
      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        return
      }

      response.next({
        kind: 'success',
        result: void 0,
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  me(): ServiceOutput<AuthenticatedUser> {
    const response = new Subject<Tried<AuthenticatedUser>>()
    const hasTokens = this.tokenPersitance.getTokens() !== undefined

    setTimeout(() => {
      if (this.MOCK_ERROR(0)) {
        response.error(new HttpErrorResponse({}))
        response.complete()
        return
      }

      if (!hasTokens) {
        response.next(new UnauthorizedError())
        response.complete()
        return
      }

      response.next({
        kind: 'success',
        result: { ...this.MOCKED_USER },
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  askToResetPassword(email: string): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    const result = validationAskResetPasswordSchema.safeParse({ email })
    setTimeout(() => {
      if (!result.success) {
        response.next(zodToExternalValidationError(result.error))
        response.complete()
        return
      }

      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        return
      }

      response.next({
        kind: 'success',
        result: void 0,
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  verifyResetToken(token: string): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    setTimeout(() => {
      if (validationVerifyResetTokenSchema.safeParse(token).success === false) {
        response.next(new ValidationError({ token: ['The reset token is invalid'] }, 'Invalid reset token'))
        response.complete()
        return
      }

      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        response.complete()
        return
      }

      response.next({
        kind: 'success',
        result: void 0,
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  resetPassword(token: string, newPassword: string): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    setTimeout(() => {
      const parseResult = validationResetPasswordSchema.safeParse({ token, newPassword })
      if (parseResult.success === false) {
        response.next(zodToExternalValidationError(parseResult.error))
        response.complete()
        return
      }

      if (this.MOCK_ERROR()) {
        response.error(new HttpErrorResponse({}))
        response.complete()
        return
      }

      response.next({
        kind: 'success',
        result: void 0,
      })
      response.complete()
    }, this.MOCK_DELAY())

    return response
  }

  private randomJwtToken(): JWT {
    const header = {
      alg: 'HS256',
      typ: 'JWT',
    }

    const now = Math.floor(Date.now() / 1000)
    const payload = {
      sub: this.MOCKED_USER.id,
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

    return `${encodedHeader}.${encodedPayload}.${fakeSignature}` as JWT
  }
}
