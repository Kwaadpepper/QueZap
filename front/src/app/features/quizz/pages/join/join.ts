import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router'

import { Message } from 'primeng/message'
import {
  catchError, filter, finalize, map, of, switchMap, tap,
} from 'rxjs'

import { ExpiredError, NotFoundError } from '@quezap/core/errors'
import { isValidSessionCode, SessionCode } from '@quezap/domain/models'
import { Spinner } from '@quezap/shared/components/spinner/spinner'

import { JoinForm } from '../../components/join-form/join-form'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-join',
  imports: [
    Message,
    JoinForm,
    Spinner,
  ],
  templateUrl: './join.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Join {
  readonly #lobbyUrl = '/quizz/lobby'
  readonly #expiredUrl = '/quizz/expired'
  private readonly router = inject(Router)
  private readonly activatedRoute = inject(ActivatedRoute)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly destroyToken = inject(DestroyRef)

  protected readonly sessionCode = signal('')
  protected readonly sessionNotFound = signal(false)
  protected readonly errorOccured = signal(false)
  protected readonly isLoading = signal(false)

  constructor() {
    this.router.events.pipe(
      takeUntilDestroyed(this.destroyToken),
      filter(event => event instanceof NavigationEnd),
      map(() => this.activatedRoute.snapshot.paramMap.get('session-code') as SessionCode | null),
      switchMap(code => this.handleSessionCode(code)),
      catchError(() => {
        this.isLoading.set(false)
        this.errorOccured.set(true)
        return []
      }),
    ).subscribe()
  }

  private handleSessionCode(code: SessionCode | null) {
    return of(code).pipe(
      tap((code) => {
        this.sessionCode.set(code ?? 'NON FOURNI')
        this.sessionNotFound.set(false)
        this.errorOccured.set(false)
      }),
      filter((code): code is SessionCode => {
        const sessionCodeIsValid = code !== null && isValidSessionCode(code)
        if (!sessionCodeIsValid) {
          this.sessionNotFound.set(true)
        }

        if (this.activatedRoute.snapshot.queryParamMap.get('failure')) {
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
