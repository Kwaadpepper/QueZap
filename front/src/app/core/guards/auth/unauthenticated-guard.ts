import { inject } from '@angular/core'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

const handleAuthenticated = (): boolean | ReturnType<Router['parseUrl']> => {
  const router = inject(Router)
  const authenticatedUser = inject(AuthenticatedUserStore)
  const authenticatedRoute = router.parseUrl('/admin')

  if (authenticatedUser.isLoggedIn()) {
    return authenticatedRoute
  }

  return true
}

export const isUnauthenticatedGuard: CanActivateFn = () => handleAuthenticated()
export const isUnauthenticatedChildGuard: CanActivateChildFn = () => handleAuthenticated()
