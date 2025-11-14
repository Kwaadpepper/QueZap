import { Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router } from '@angular/router'

import { Message } from 'primeng/message'

import { ForbidenError } from '@quezap/core/errors'
import { zod } from '@quezap/core/tools'
import { isSuccess } from '@quezap/core/types'

import { ResetPasswordForm } from '../../components/reset-password-form/reset-password-form'
import { AUTHENTICATION_SERVICE } from '../../services'

@Component({
  selector: 'quizz-reset-password',
  imports: [ResetPasswordForm, Message],
  templateUrl: './reset-password.html',
})
export class ResetPassword {
  private readonly router = inject(Router)
  private readonly authenticationService = inject(AUTHENTICATION_SERVICE)

  private readonly validTokenSchema = zod.jwt()
  protected readonly resetToken = signal('')

  protected readonly anErrorOccured = signal(false)
  private readonly tokenIsInvalid = signal(false)
  protected readonly displayForm = computed(() =>
    !this.anErrorOccured() && !this.tokenIsInvalid(),
  )

  constructor() {
    const queryResetToken = this.router.currentNavigation()?.extractedUrl?.queryParamMap.get('token') || ''
    const resetToken = this.validTokenSchema.safeParse(queryResetToken).success ? queryResetToken : null

    if (resetToken === null) {
      this.tokenIsInvalid.set(true)
      return
    }

    this.verifyToken(resetToken)
  }

  private verifyToken(token: string) {
    this.authenticationService.verifyResetToken(token)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (result) => {
          if (isSuccess(result)) {
            this.resetToken.set(token)
            return
          }

          const isForbiddenError = result.error instanceof ForbidenError
          this.tokenIsInvalid.set(isForbiddenError)
          this.anErrorOccured.set(!isForbiddenError)
        },
        error: (e) => {
        // Gère toute erreur de l'Observable lui-même (erreur technique 5xx)
          console.error('Erreur technique de l\'Observable :', e)
          this.anErrorOccured.set(true) // Affiche l'erreur générale
        },
      })
  }
}
