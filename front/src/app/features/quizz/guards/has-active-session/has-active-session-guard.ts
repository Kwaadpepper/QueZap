import { inject } from '@angular/core'
import { toObservable } from '@angular/core/rxjs-interop'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router' // Importez les types Fn

import { filter, map, Observable, take } from 'rxjs'

import { ActiveSessionStore } from '../../stores'

const handleMissingActiveSession = (): Observable<boolean | ReturnType<Router['parseUrl']>> => {
  const router = inject(Router)
  const sessionStore = inject(ActiveSessionStore)
  const unAuthRoute = router.parseUrl('/quizz/expired')

  return toObservable(sessionStore.restorationComplete).pipe(
    filter(isComplete => isComplete),
    take(1),
    map(() => {
      return sessionStore.session()
        ? true
        : unAuthRoute
    }),
  )
}

export const hasActiveSessionGuard: CanActivateFn = () => handleMissingActiveSession()
export const hasActiveSessionChildGuard: CanActivateChildFn = () => handleMissingActiveSession()
