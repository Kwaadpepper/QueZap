import { InjectionToken } from '@angular/core'

import { ForbidenError } from '@quezap/core/errors'
import { UnauthorizedError } from '@quezap/core/errors/unauthorized-error'
import { type IS_REFRESHING_ACCESS_TOKEN as _IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { ServiceOutput } from '@quezap/core/types'
import { AuthenticatedUser } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'

export interface AuthenticationService {
  login(email: string, password: string): ServiceOutput<AuthTokens, UnauthorizedError>

  /**
   * @important
   * In order to void recursivity : You MUST ensure that only one refresh process is running at a time.
   * Use the {@link _IS_REFRESHING_ACCESS_TOKEN} injection token to track the refresh state.
   */
  refresh(tokens: AuthTokens): ServiceOutput<AuthTokens>

  logout(): ServiceOutput<void>

  me(): ServiceOutput<AuthenticatedUser, ForbidenError>

  askToResetPassword(email: string): ServiceOutput<void>

  verifyResetToken(token: string): ServiceOutput<void, ForbidenError>

  resetPassword(token: string, newPassword: string): ServiceOutput<void>
}

export const AUTHENTICATION_SERVICE = new InjectionToken<AuthenticationService>('AuthenticationService')
