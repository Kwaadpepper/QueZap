import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'

import { Message } from 'primeng/message'
import { ProgressSpinner } from 'primeng/progressspinner'
import { catchError, filter, map, switchMap, tap } from 'rxjs'

import { isValidSessionCode, SessionCode } from '@quezap/domain/models'

import { JoinForm } from '../../components'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-join',
  imports: [
    Message,
    ProgressSpinner,
    JoinForm,
  ],
  templateUrl: './join.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Join {
  readonly #lobbyUrl = '/quizz/lobby'
  private readonly router = inject(Router)
  private readonly activatedRoute = inject(ActivatedRoute)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly destroyToken = inject(DestroyRef)

  protected readonly sessionCode = signal('')
  protected readonly sessionNotFound = signal(false)
  protected readonly isLoading = signal(false)

  constructor() {
    this.activatedRoute.paramMap.pipe(
      map(params => params.get('session-code') as SessionCode | null),
      tap((code) => {
        this.sessionCode.set(code ?? 'NON FOURNI')
        this.sessionNotFound.set(false)
        this.isLoading.set(false)
      }),
      filter((code): code is SessionCode => {
        const sessionCodeIsValid = code !== null && isValidSessionCode(code)
        if (!sessionCodeIsValid) {
          this.sessionNotFound.set(true)
        }
        return sessionCodeIsValid
      }),
      switchMap(code => this.loadSession(code).pipe(
        catchError(() => {
          this.isLoading.set(false)
          this.sessionNotFound.set(true)
          return []
        }),
      )),
      takeUntilDestroyed(this.destroyToken),
    ).subscribe()
  }

  private loadSession(code: SessionCode) {
    this.isLoading.set(true)

    return this.sessionStore.startSession(code).pipe(
      tap(() => {
        this.isLoading.set(false)
        this.router.navigate([this.#lobbyUrl])
      }),
    )
  }
}
