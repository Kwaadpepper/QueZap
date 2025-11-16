import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'

import { Message } from 'primeng/message'
import { ProgressSpinner } from 'primeng/progressspinner'
import { catchError, firstValueFrom, map, of } from 'rxjs'

import { isFailure } from '@quezap/core/types'
import { isValidSessionCode, SessionCode } from '@quezap/domain/models'

import { SESSION_SERVICE, SessionMockService } from '../../services'

@Component({
  selector: 'quizz-join',
  imports: [Message, ProgressSpinner],
  templateUrl: './join.html',
  providers: [
    { provide: SESSION_SERVICE, useClass: SessionMockService },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Join {
  private readonly activatedRoute = inject(ActivatedRoute)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly destroyToken = inject(DestroyRef)

  protected readonly sessionCode = signal('')
  protected readonly sessionNotFound = signal(false)
  protected readonly isLoading = signal(false)

  constructor() {
    const sessionCode = this.activatedRoute.snapshot.params['session-code']

    if (sessionCode && isValidSessionCode(sessionCode)) {
      this.sessionCode.set(sessionCode)
      this.loadSession(sessionCode)
      return
    }

    this.sessionNotFound.set(true)
  }

  private loadSession(code: SessionCode) {
    firstValueFrom(
      this.sessionService.find(code).pipe(
        takeUntilDestroyed(this.destroyToken),
        map((session) => {
          if (isFailure(session)) {
            this.sessionNotFound.set(true)
            return
          }

          alert(`Session trouvée : ${session.result.name}`)
        }),
        catchError(() => {
          this.sessionNotFound.set(true)

          return of(void 0)
        }),
      ),
    )
  }
}
