import { Component, computed, ErrorHandler, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router } from '@angular/router'

import { MessageModule } from 'primeng/message'
import { ProgressSpinner } from 'primeng/progressspinner'
import {
  catchError, finalize, firstValueFrom, of, retry, tap,
} from 'rxjs'
import * as zod from 'zod/v4'

import { ForbidenError, HandledFrontError } from '@quezap/core/errors'
import { isSuccess } from '@quezap/core/types'

import { ResetPasswordForm } from '../../components/reset-password-form/reset-password-form'
import { AUTHENTICATION_SERVICE } from '../../services'

@Component({
  selector: 'quizz-reset-password',
  imports: [
    ResetPasswordForm,
    MessageModule,
    ProgressSpinner,
  ],
  templateUrl: './reset-password.html',
})
export class ResetPassword {
  private readonly router = inject(Router)
  private readonly authenticationService = inject(AUTHENTICATION_SERVICE)
  private readonly errorHandler = inject(ErrorHandler)

  private readonly validTokenSchema = zod.jwt()
  protected readonly resetToken = signal('')

  protected readonly verifyingToken = signal(true)
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
    firstValueFrom(
      this.authenticationService.verifyResetToken(token).pipe(
        retry(1),
        takeUntilDestroyed(),
        tap((result) => {
          if (isSuccess(result)) {
            this.resetToken.set(token)
            return
          }

          const isForbiddenError = result.error instanceof ForbidenError
          this.tokenIsInvalid.set(isForbiddenError)
          this.anErrorOccured.set(!isForbiddenError)
        }),
        catchError((err) => {
          this.anErrorOccured.set(true)
          this.errorHandler.handleError(
            HandledFrontError.from(err),
          )

          return of(void 0)
        }),
        finalize(() => {
          this.verifyingToken.set(false)
        }),
      ),
    )
  }
}
