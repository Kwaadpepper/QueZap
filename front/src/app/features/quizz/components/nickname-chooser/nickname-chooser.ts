import { ChangeDetectionStrategy, Component, computed, ErrorHandler, inject, signal } from '@angular/core'
import { Field, form, minLength, required, submit } from '@angular/forms/signals'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { CheckboxModule } from 'primeng/checkbox'
import { InputText } from 'primeng/inputtext'
import { catchError, exhaustMap, finalize, firstValueFrom, of } from 'rxjs'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services'
import { FieldError } from '@quezap/shared/directives'

import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-nickname-chooser',
  imports: [
    Button, Field, FieldError, InputText, CheckboxModule,
  ],
  templateUrl: './nickname-chooser.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NicknameChooser {
  readonly #mockedPseudo = 'Alice'
  private readonly errorHandler = inject(ErrorHandler)
  private readonly config = inject(Config)
  private readonly message = inject(MessageService)
  private readonly sessionStore = inject(ActiveSessionStore)

  protected readonly loading = signal(false)
  protected readonly nicknameValidated = signal(false)
  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly selectedNickname = computed(() => this.sessionStore.nickname()?.value())

  private readonly nickname = signal({
    nicknameValue: this.sessionStore.nickname()?.value() ?? '',
    rememberMe: this.sessionStore.nickname()?.remembered() ?? false,
  })

  protected nicknameForm = form(this.nickname, (path) => {
    required(path.nicknameValue, { message: 'Le pseudo est requis' })
    minLength(path.nicknameValue, 3, { message: 'Le pseudo doit faire au moins 3 caractères' })
  })

  protected onChooseNickname(): void {
    if (this.nicknameForm().invalid()) {
      return
    }
    this.loading.set(true)

    submit(this.nicknameForm, form =>
      firstValueFrom(this.sessionStore.chooseNickname(
        form.nicknameValue().value(),
        form.rememberMe().value(),
      ).pipe(
        exhaustMap((result) => {
          if (result instanceof ValidationError) {
            return of(result.getErrorsForForm(this.nicknameForm))
          }

          this.nicknameValidated.set(true)

          return of(void 0)
        }),
        catchError((err) => {
          this.message.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Une erreur est survenue lors de la prise de pseudo. Veuillez réessayer.',
            life: 5000,
          })

          this.errorHandler.handleError(
            HandledFrontError.from(err),
          )

          return of(void 0)
        }),
        finalize(() => this.loading.set(false)),
      )),
    )
  }

  protected onFillMockedValue() {
    this.nickname.update(() => ({
      ...this.nickname(),
      nicknameValue: this.#mockedPseudo,
    }))
  }
}
