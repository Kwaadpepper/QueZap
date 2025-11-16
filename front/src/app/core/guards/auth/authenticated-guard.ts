import { inject } from '@angular/core'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

const handleUnauthenticated = (): boolean | ReturnType<Router['parseUrl']> => {
  const router = inject(Router)
  const authenticatedUser = inject(AuthenticatedUserStore)
  const unAuthRoute = router.parseUrl('/auth/login')

  if (authenticatedUser.isLoggedIn()) {
    return true
  }

  return unAuthRoute
}

export const isAuthenticatedGuard: CanActivateFn = () => handleUnauthenticated()
export const isAuthenticatedChildGuard: CanActivateChildFn = () => handleUnauthenticated()
