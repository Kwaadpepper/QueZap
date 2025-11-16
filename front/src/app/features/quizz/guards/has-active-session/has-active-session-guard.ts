import { inject } from '@angular/core'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router' // Importez les types Fn

import { ActiveSessionStore } from '../../stores'

const handleMissingActiveSession = (): boolean | ReturnType<Router['parseUrl']> => {
  const router = inject(Router)
  const sessionStore = inject(ActiveSessionStore)
  const unAuthRoute = router.parseUrl('/quizz/expired')

  if (sessionStore.session()) {
    return true
  }

  return unAuthRoute
}

export const hasActiveSessionGuard: CanActivateFn = () => handleMissingActiveSession()
export const hasActiveSessionChildGuard: CanActivateChildFn = () => handleMissingActiveSession()
