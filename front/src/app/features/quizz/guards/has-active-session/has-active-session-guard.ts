import { inject } from '@angular/core'
import { toObservable } from '@angular/core/rxjs-interop'
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router' // Importez les types Fn

import { filter, map, Observable, take } from 'rxjs'

import { Session } from '@quezap/domain/models'

import { ActiveSessionStore } from '../../stores'

const handleMissingActiveSession = (options?: GuardOptions) =>
  (): Observable<boolean | ReturnType<Router['parseUrl']>> => {
    const router = inject(Router)
    const sessionStore = inject(ActiveSessionStore)
    const unAuthRoute = router.parseUrl('/quizz/expired')

    // ? Session restoration already done
    if (sessionStore.restorationComplete()) {
      return new Observable<boolean | ReturnType<Router['parseUrl']>>((subscriber) => {
        subscriber.next(
          isSessionActive(sessionStore.session(), options)
            ? true
            : unAuthRoute,
        )
        subscriber.complete()
      })
    }

    // * Wait for session restoration to complete
    return toObservable(sessionStore.restorationComplete).pipe(
      filter(isComplete => isComplete),
      take(1),
      map(() => {
        return isSessionActive(sessionStore.session(), options)
          ? true
          : unAuthRoute
      }),
    )
  }

function isSessionActive(
  session: Session | undefined,
  options?: GuardOptions,
): boolean {
  if (session === undefined) {
    return false
  }

  if (options?.withQuizzRunning) {
    return session.endedAt === null || session.startedAt !== null
  }

  return true
}

interface GuardOptions {
  withQuizzRunning?: true
}

export const hasActiveSessionGuard: (options?: GuardOptions) => CanActivateFn
  = handleMissingActiveSession
export const hasActiveSessionChildGuard: (options?: GuardOptions) => CanActivateChildFn
  = handleMissingActiveSession
