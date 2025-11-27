import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router } from '@angular/router'

import { Message } from 'primeng/message'
import {
  catchError, filter, finalize, map, of, switchMap, tap,
} from 'rxjs'

import { ExpiredError, NotFoundError } from '@quezap/core/errors'
import { isValidSessionCode, SessionCode } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { Spinner } from '@quezap/shared/components/spinner/spinner'

import { JoinForm } from '../../components/join-form/join-form'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-join',
  imports: [
    Message,
    JoinForm,
    Spinner,
    IconFacade,
  ],
  templateUrl: './join.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Join {
  readonly #lobbyUrl = '/quizz/lobby'
  readonly #expiredUrl = '/quizz/expired'
  private readonly router = inject(Router)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly destroyToken = inject(DestroyRef)

  readonly failure = input<boolean>()
  readonly 'session-code' = input<SessionCode>()
  protected readonly sessionCode = computed(() => this['session-code']())

  protected readonly sessionNotFound = signal(false)
  protected readonly errorOccured = signal(false)
  protected readonly isLoading = signal(false)

  constructor() {
    this.router.events.pipe(
      takeUntilDestroyed(this.destroyToken),
      filter(event => event instanceof NavigationEnd),
      map(() => this.sessionCode()),
      switchMap(code => this.handleSessionCode(code)),
      catchError(() => {
        this.isLoading.set(false)
        this.errorOccured.set(true)

        return []
      }),
    ).subscribe()
  }

  private handleSessionCode(code: SessionCode | undefined) {
    return of(code).pipe(
      tap(() => {
        this.sessionNotFound.set(false)
        this.errorOccured.set(false)
      }),
      filter((code): code is SessionCode => {
        const sessionCodeIsValid = code !== undefined && isValidSessionCode(code)
        if (!sessionCodeIsValid) {
          this.sessionNotFound.set(true)
        }

        if (this.failure()) {
          this.errorOccured.set(true)
          return false
        }
        return sessionCodeIsValid
      }),
      switchMap(code => this.loadSession(code)),
    )
  }

  private loadSession(code: SessionCode) {
    this.isLoading.set(true)
    return this.sessionStore.joinSession(code).pipe(
      tap((result) => {
        this.isLoading.set(false)

        if (result instanceof NotFoundError) {
          this.sessionNotFound.set(true)
          return
        }

        if (result instanceof ExpiredError) {
          setTimeout(() => {
            this.router.navigate([this.#expiredUrl], { skipLocationChange: true })
          }, 0)
          return
        }

        setTimeout(() => {
          this.router.navigate([this.#lobbyUrl], { skipLocationChange: true })
        }, 0)
      }),
      catchError(() => {
        this.isLoading.set(false)
        this.errorOccured.set(true)
        return []
      }),
      finalize(() => this.isLoading.set(false)),
    )
  }
}
