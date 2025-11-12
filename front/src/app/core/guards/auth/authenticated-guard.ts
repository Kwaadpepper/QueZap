import { inject, Injectable } from '@angular/core'
import { CanActivate, CanActivateChild, GuardResult, MaybeAsync, Router } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

@Injectable({
  providedIn: 'root',
})
export class AuthenticatedGuard implements CanActivate, CanActivateChild {
  private readonly router = inject(Router)
  private readonly authenticatedUser = inject(AuthenticatedUserStore)
  private readonly unAuthRoute = this.router.parseUrl('/auth/login')

  canActivate(): MaybeAsync<GuardResult> {
    return this.handleUnauthenticated()
  }

  canActivateChild(): MaybeAsync<GuardResult> {
    return this.handleUnauthenticated()
  }

  private handleUnauthenticated(): GuardResult {
    if (this.authenticatedUser.isLoggedIn()) {
      return true
    }

    return this.unAuthRoute
  }
}
