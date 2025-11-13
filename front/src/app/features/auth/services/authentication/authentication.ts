import { InjectionToken } from '@angular/core'

import { Observable } from 'rxjs'

import { type IS_REFRESHING_ACCESS_TOKEN as _IS_REFRESHING_ACCESS_TOKEN } from '@quezap/core/tokens'
import { AuthenticatedUser } from '@quezap/domain/models'
import { AuthTokens } from '@quezap/domain/models/auth-tokens'

export interface AuthenticationService {
  login(email: string, password: string): Observable<AuthTokens>

  /**
   * @important
   * In order to void recursivity : You MUST ensure that only one refresh process is running at a time.
   * Use the {@link _IS_REFRESHING_ACCESS_TOKEN} injection token to track the refresh state.
   */
  refresh(tokens: AuthTokens): Observable<AuthTokens>

  logout(): Observable<void>

  me(): Observable<AuthenticatedUser>

  resetPassword(email: string): Observable<void>
}

export const AUTHENTICATION_SERVICE = new InjectionToken<AuthenticationService>('AuthenticationService')
