import { Component, computed, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'

import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Message } from 'primeng/message'
import { catchError, firstValueFrom, of, tap } from 'rxjs'

import { Config } from '@quezap/core/services'
import { zod } from '@quezap/core/tools'
import { BackButton } from '@quezap/shared/components'
import { FieldError } from '@quezap/shared/directives'

import { AUTHENTICATION_SERVICE } from '../../services'

@Component({
  selector: 'quizz-forgotten-password',
  imports: [InputText, Field, Button, Message, BackButton, FieldError],
  templateUrl: './forgotten-password.html',
})
export class ForgottenPassword {
  private readonly config = inject(Config)
  private readonly authenticationService = inject(AUTHENTICATION_SERVICE)
  private readonly destroyRef = inject(DestroyRef)

  readonly #mockedCredentials = {
    email: 'user@example.net',
  }

  private readonly resetInfo = signal({
    email: '',
  })

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly hasBeenAskedToReset = signal(false)
  protected readonly errorHasOccured = signal(false)

  protected readonly resetForm = form(this.resetInfo, (path) => {
    validateStandardSchema(path, zod.object({
      email: zod.email('Email invalide'),
    }))
  })

  protected onAskToReset() {
    if (this.resetForm().invalid()) {
      this.resetForm().markAsTouched()
      return
    }

    this.hasBeenAskedToReset.set(false)
    this.errorHasOccured.set(false)

    submit(this.resetForm, async form =>
      firstValueFrom(
        this.authenticationService.askToResetPassword(
          form.email().value(),
        ).pipe(
          takeUntilDestroyed(this.destroyRef),
          tap(() => {
            this.resetFormInput()
            this.resetForm().reset()
            this.hasBeenAskedToReset.set(true)
          }),
          catchError(() => {
            this.resetForm().reset()
            this.resetFormInput()
            this.errorHasOccured.set(true)
            return of(null)
          }),
        ),
      ),
    )
  }

  protected onFillMockedValue() {
    this.resetInfo.set({
      ...this.#mockedCredentials,
    })
    this.resetForm().markAsDirty()
  }

  private resetFormInput() {
    this.resetInfo.set({
      email: '',
    })
    this.resetForm().markAsDirty()
  }
}
