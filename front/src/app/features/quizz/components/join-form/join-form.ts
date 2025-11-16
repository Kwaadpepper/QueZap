import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { customError, Field, form, validate } from '@angular/forms/signals'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'

import { Config } from '@quezap/core/services'
import { isValidSessionCode } from '@quezap/domain/models'
import { FieldError } from '@quezap/shared/directives'

@Component({
  selector: 'quizz-join-form',
  imports: [
    Button,
    InputText,
    FieldError,
    Field,
  ],
  templateUrl: './join-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JoinForm {
  readonly #joinUrl = '/quizz/join'
  private readonly router = inject(Router)
  private readonly config = inject(Config)

  public readonly invalidCodes = input<string[]>([])

  protected readonly isDebug = computed(() => this.config.debug())

  protected readonly joinCode = signal({
    code: '',
  })

  protected readonly joinCodeForm = form(this.joinCode, (path) => {
    validate(path.code, ({ value }) => {
      return isValidSessionCode(value())
        && !this.invalidCodes().includes(value())
        ? []
        : customError({
            kind: 'invalid-value',
            message: 'Le code de session est invalide.',
          })
    })
  })

  protected onSubmitJoinForm() {
    if (this.joinCodeForm().invalid()) {
      console.warn('Formulaire invalide, soumission annulée.')
      return
    }

    this.router.navigate([this.#joinUrl, this.joinCodeForm().value().code])
  }

  protected onFillMockedValue() {
    this.joinCode.update(() => ({
      code: 'A2B3C4',
    }))
  }
}
