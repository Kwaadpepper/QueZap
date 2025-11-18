import { inject } from '@angular/core'
import { toObservable } from '@angular/core/rxjs-interop'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router' // Importez les types Fn

import { filter, map, Observable, take } from 'rxjs'

import { ActiveSessionStore } from '../../stores'

const handleMissingActiveSession = (options?: GuardOptions) =>
  (): Observable<boolean | ReturnType<Router['parseUrl']>> => {
    const router = inject(Router)
    const sessionStore = inject(ActiveSessionStore)
    const unAuthRoute = router.parseUrl('/quizz/expired')

    return toObservable(sessionStore.restorationComplete).pipe(
      filter(isComplete => isComplete),
      take(1),
      map(() => {
        const session = sessionStore.session()
        if (session === undefined) {
          return unAuthRoute
        }
        if (options?.withQuizzRunning) {
          return session.endedAt === null || session.startedAt !== null
            ? true
            : unAuthRoute
        }

        return true
      }),
    )
  }

interface GuardOptions {
  withQuizzRunning?: true
}

export const hasActiveSessionGuard: (options?: GuardOptions) => CanActivateFn
  = handleMissingActiveSession
export const hasActiveSessionChildGuard: (options?: GuardOptions) => CanActivateChildFn
  = handleMissingActiveSession
