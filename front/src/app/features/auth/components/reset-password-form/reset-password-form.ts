import {
  Component, computed, ErrorHandler, inject, input, signal,
} from '@angular/core'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Message } from 'primeng/message'
import { catchError, firstValueFrom, of, tap } from 'rxjs'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services'
import { zod } from '@quezap/core/tools'
import { isFailure } from '@quezap/core/types'
import { FieldError } from '@quezap/shared/directives'

import { AUTHENTICATION_SERVICE } from '../../services'

@Component({
  selector: 'quizz-reset-password-form',
  imports: [
    Message,
    InputText,
    Field,
    FieldError,
    Button,
  ],
  templateUrl: './reset-password-form.html',
})
export class ResetPasswordForm {
  private readonly authentication$ = inject(AUTHENTICATION_SERVICE)
  private readonly message$ = inject(MessageService)
  private readonly router = inject(Router)
  private readonly config = inject(Config)
  private readonly errorHandler = inject(ErrorHandler)

  readonly #mockedValues = {
    password: 'NewPassword123!',
    confirmPassword: 'NewPassword123!',
  }

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly anErrorOccured = signal(false)

  protected readonly resetValues = signal({
    password: '',
    confirmPassword: '',
  })

  protected readonly resetForm = form(this.resetValues, (path) => {
    validateStandardSchema(path, zod.object({
      password: zod.string().nonempty('Vous devez entrer un mot de passe'),
      confirmPassword: zod.string().nonempty('Vous devez confirmer le mot de passe')
        .refine(val => val === this.resetValues().password, { message: 'Les mots de passe ne correspondent pas' }),
    }))
  })

  public readonly resetToken = input.required<string>()

  protected onReset(): void {
    if (!this.resetForm().valid()) {
      this.resetForm().markAsTouched()
      return
    }

    this.anErrorOccured.set(false)

    submit(this.resetForm, async form =>
      firstValueFrom(
        this.authentication$.resetPassword(
          this.resetToken(),
          form.password().value(),
        ).pipe(
          tap((result) => {
            this.resetFormInput()
            this.resetForm().reset()

            if (isFailure(result)) {
              if (result.error instanceof ValidationError) {
                return of(result.error.getErrorsForForm(this.resetForm))
              }

              this.anErrorOccured.set(true)
              return of(void 0)
            }

            this.message$.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Votre mot de passe a été réinitialisé avec succès.',
            })

            this.redirectToLogin()

            return of(void 0)
          }),
          catchError((err) => {
            this.resetForm().reset()
            this.resetFormInput()

            this.message$.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'La réinitialisation du mot de passe a échoué. Veuillez réessayer.',
            })

            this.errorHandler.handleError(
              HandledFrontError.from(err),
            )

            return of(void 0)
          }),
        ),
      ),
    )
  }

  protected onFillMockedValues() {
    this.resetValues.set(this.#mockedValues)
    this.resetForm().markAsDirty()
  }

  private redirectToLogin() {
    this.router.navigateByUrl('/auth/login')
  }

  private resetFormInput() {
    this.resetValues.set({
      password: '',
      confirmPassword: '',
    })
    this.resetForm().reset()
  }
}
