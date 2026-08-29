import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { form, FormField, validate } from '@angular/forms/signals'
import { Router } from '@angular/router'

import { Button, ButtonIcon } from 'primeng/button'
import { InputText } from 'primeng/inputtext'

import { Config } from '@quezap/core/services/config/config'
import { isValidSessionCode } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { FieldError } from '@quezap/shared/directives/field-error'

@Component({
  selector: 'quizz-join-form',
  imports: [
    Button,
    InputText,
    FieldError,
    IconFacade,
    ButtonIcon,
    FormField,
  ],
  templateUrl: './join-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JoinForm {
  readonly #joinUrl = '/quizz/join'
  readonly #mockedCode = 'A2B3C4'
  private readonly router = inject(Router)
  private readonly config = inject(Config)

  protected readonly isDebug = computed(() => this.config.debug())

  protected readonly joinCode = signal({ code: '' })

  protected readonly joinCodeForm = form(this.joinCode, (path) => {
    validate(path.code, ({ value }) => {
      const code = (value() ?? '').trim().toUpperCase()
      return isValidSessionCode(code)
        ? []
        : {
            kind: 'invalid-value',
            message: 'Le code PIN est invalide (ex: QZ-12345 ou AB12CD).',
          }
    })
  })

  protected onSubmitJoinForm() {
    if (this.joinCodeForm().invalid()) {
      console.warn('Formulaire invalide, soumission annulée.')
      return
    }

    const code = this.joinCodeForm().value().code.trim().toUpperCase()
    this.router.navigate([this.#joinUrl, code], { onSameUrlNavigation: 'reload' })
  }

  protected onFillMockedValue() {
    this.joinCode.update(() => ({ code: this.#mockedCode }))
    this.joinCodeForm().markAsDirty()
    this.joinCodeForm().markAsTouched()
  }
}
