import { Component, DestroyRef, ErrorHandler, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router } from '@angular/router'

import { MessageService } from '@openng/optimus-ui/api'
import { ButtonModule } from '@openng/optimus-ui/button'

import { catchError, firstValueFrom, of, tap } from 'rxjs'

import { HandledFrontError } from '@quezap/core/errors'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

@Component({
  selector: 'quizz-logout-button',
  imports: [
    ButtonModule,
    IconFacade,
  ],
  templateUrl: './logout-button.html',
})
export class LogoutButton {
  private readonly router = inject(Router)
  private readonly messageService = inject(MessageService)
  private readonly authenticatedUser = inject(AuthenticatedUserStore)
  private readonly errorHandler = inject(ErrorHandler)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly running = signal(false)

  protected onLogout() {
    this.running.set(true)
    firstValueFrom(
      this.authenticatedUser.logout()
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          catchError((err) => {
            this.errorHandler.handleError(
              HandledFrontError.from(err),
            )

            return of(void 0)
          }),
          tap(() => {
            this.router.navigate(['/auth/login'])
            this.messageService.add({
              severity: 'info',
              summary: 'Déconnexion',
              detail: 'Vous avez été déconnecté.',
            })
            this.running.set(false)
          }),
        ),
      { defaultValue: undefined },
    )
  }
}
