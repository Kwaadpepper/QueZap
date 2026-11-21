import {
  Component, computed, DestroyRef, ErrorHandler, inject, model, signal, viewChild,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { form, FormField, submit, validateStandardSchema } from '@angular/forms/signals'

import { MessageService } from '@openng/optimus-ui/api'
import { Button, ButtonIcon } from '@openng/optimus-ui/button'
import { Dialog } from '@openng/optimus-ui/dialog'
import { InputText } from '@openng/optimus-ui/inputtext'

import { catchError, exhaustMap, firstValueFrom, of, throwError } from 'rxjs'
import * as zod from 'zod/v4'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { isFailure } from '@quezap/core/types'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { TurnstileWidget } from '@quezap/shared/components/turnstile/turnstile'
import { FieldError } from '@quezap/shared/directives/field-error'

import { REGISTER_SERVICE } from '../../services'

@Component({
  selector: 'quizz-register-modal',
  imports: [
    Dialog,
    InputText,
    Button,
    FormField,
    FieldError,
    IconFacade,
    ButtonIcon,
    TurnstileWidget,
  ],
  providers: [],
  templateUrl: './register-modal.html',
})
export class RegisterModal {
  private readonly message = inject(MessageService)
  private readonly registerService = inject(REGISTER_SERVICE)
  private readonly errorHandler = inject(ErrorHandler)
  private readonly destroyRef = inject(DestroyRef)

  private readonly userInfo = signal({ email: '', password: '', passwordConfirmation: '' })

  protected readonly registerForm = form(this.userInfo, (path) => {
    validateStandardSchema(
      path,
      zod
        .object({
          email: zod.email('Email invalide'),
          password: zod
            .string()
            .min(8, 'Au moins 8 caractères')
            .max(80, 'Au plus 80 caractères')
            .regex(/[a-z]/, 'Au moins une minuscule')
            .regex(/[A-Z]/, 'Au moins une majuscule')
            .regex(/\d/, 'Au moins un chiffre')
            .regex(/[!-/:-@[-`{-~]/, 'Au moins un caractère spécial'),
          passwordConfirmation: zod.string(),
        })
        .refine(value => value.password === value.passwordConfirmation, {
          path: ['passwordConfirmation'],
          message: 'Les mots de passe ne correspondent pas',
        }),
    )
  })

  private readonly turnstileWidget = viewChild(TurnstileWidget)

  public readonly visible = model(false)

  protected readonly turnstileToken = signal<string | null>(null)
  protected readonly isTurnstilePending = computed(
    () => Boolean(this.turnstileWidget()?.enabled()) && !this.turnstileToken(),
  )

  protected onTurnstileToken(token: string | null) {
    this.turnstileToken.set(token)
  }

  protected onModalHide() {
    this.userInfo.set({ email: '', password: '', passwordConfirmation: '' })
    this.registerForm().reset()
    this.turnstileToken.set(null)
    this.turnstileWidget()?.reset()
  }

  protected onRegister() {
    if (this.registerForm().invalid() || this.isTurnstilePending()) {
      this.registerForm().markAsTouched()
      return
    }

    const token = this.turnstileToken()

    submit(this.registerForm, form =>
      firstValueFrom(
        (token
          ? this.registerService.register(form.email().value(), form.password().value(), token)
          : this.registerService.register(form.email().value(), form.password().value())
        ).pipe(
          takeUntilDestroyed(this.destroyRef),
          exhaustMap((result) => {
            if (isFailure(result)) {
              this.turnstileWidget()?.reset()
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
            this.turnstileWidget()?.reset()
            this.message.add({
              severity: 'error',
              summary: 'Erreur lors de l\'inscription',
              detail: 'Une erreur est survenue lors de l\'envoi du formulaire',
              life: 5000,
            })

            this.errorHandler.handleError(
              HandledFrontError.from(err),
            )

            return of(void 0)
          }),
        ),
        { defaultValue: undefined },
      ),
    )
  }

  protected onCancel() {
    this.visible.set(false)
  }
}
