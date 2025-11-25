import { Component, ErrorHandler, inject, model, signal } from '@angular/core'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'

import { MessageService } from 'primeng/api'
import { Button, ButtonIcon } from 'primeng/button'
import { Dialog } from 'primeng/dialog'
import { InputText } from 'primeng/inputtext'
import { catchError, exhaustMap, firstValueFrom, of, throwError } from 'rxjs'
import * as zod from 'zod/v4'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { FieldError } from '@quezap/shared/directives/field-error'

import { REGISTER_SERVICE, RegisterMockService } from '../../services'

@Component({
  selector: 'quizz-register-modal',
  imports: [
    Dialog,
    InputText,
    Button,
    Field,
    FieldError,
    IconFacade,
    ButtonIcon,
  ],
  providers: [
    {
      provide: REGISTER_SERVICE,
      useClass: RegisterMockService,
    },
  ],
  templateUrl: './register-modal.html',
})
export class RegisterModal {
  private readonly message = inject(MessageService)
  private readonly registerService = inject(REGISTER_SERVICE)
  private readonly errorHandler = inject(ErrorHandler)

  private readonly userInfo = signal({ email: '' })

  protected readonly registerForm = form(this.userInfo, (path) => {
    validateStandardSchema(path, zod.object({ email: zod.email('Email invalide') }))
  })

  public readonly visible = model(false)

  protected onModalHide() {
    this.userInfo.set({ email: '' })
    this.registerForm().reset()
  }

  protected onRegister() {
    if (this.registerForm().invalid()) {
      this.registerForm().markAsTouched()
      return
    }

    submit(this.registerForm, form =>
      firstValueFrom(this.registerService.register(
        form.email().value(),
      ).pipe(
        exhaustMap((result) => {
          if (isFailure(result)) {
            const err = result.error
            if (err instanceof ValidationError) {
              return of(err.getErrorsForForm(this.registerForm))
            }

            // Unexpected error
            return throwError(() => err)
          }

          this.message.add({
            severity: 'success',
            summary: 'Inscription réussie',
            detail: 'Vous allez recevoir un email contenant votre lien d\'activation',
            sticky: true,
          })
          this.visible.set(false)

          return of(void 0)
        }),
        catchError((err) => {
          this.message.add({
            severity: 'error',
            summary: 'Erreur lors de l\'inscription',
            detail: 'Un erreur est survenu lors de l\'envoi du formulaire',
            life: 5000,
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

  protected onCancel() {
    this.visible.set(false)
  }
}
