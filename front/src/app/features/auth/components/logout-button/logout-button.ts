import { Component, inject, signal } from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button, ButtonIcon } from 'primeng/button'
import { catchError, firstValueFrom, tap, throwError } from 'rxjs'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

@Component({
  selector: 'quizz-logout-button',
  imports: [
    Button,
    ButtonIcon,
  ],
  templateUrl: './logout-button.html',
})
export class LogoutButton {
  private readonly router = inject(Router)
  private readonly messageService = inject(MessageService)
  private readonly authenticatedUser = inject(AuthenticatedUserStore)

  protected readonly running = signal(false)

  protected onLogout() {
    this.running.set(true)
    firstValueFrom(
      this.authenticatedUser.logout()
        .pipe(
          tap(() => {
            this.router.navigate(['/auth/login'])
            this.messageService.add({
              severity: 'info',
              summary: 'Deconnexion',
              detail: 'Vous avez été déconnecté.',
            })
            this.running.set(false)
          }),
          catchError((error) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Une erreur est survenue lors de la déconnexion.',
            })
            this.running.set(false)

            return throwError(() => error)
          }),
        ),
    )
  }
}
