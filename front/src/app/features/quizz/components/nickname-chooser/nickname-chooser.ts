import {
  ChangeDetectionStrategy, Component, computed, effect, ErrorHandler, inject, signal,
} from '@angular/core'
import {
  form, FormField, maxLength, minLength, required, submit,
} from '@angular/forms/signals'

import { MessageService } from 'primeng/api'
import { Button, ButtonIcon } from 'primeng/button'
import { CheckboxModule } from 'primeng/checkbox'
import { InputText } from 'primeng/inputtext'
import { catchError, exhaustMap, finalize, firstValueFrom, of } from 'rxjs'

import { ForbidenError, HandledFrontError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { FieldError } from '@quezap/shared/directives/field-error'

import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-nickname-chooser',
  imports: [
    Button, FieldError, FormField, InputText, CheckboxModule,
    IconFacade,
    ButtonIcon,
    FormField,
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
  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly selectedNickname = computed(() => this.sessionStore.nickname()?.value())
  protected readonly nicknameValidated = signal(this.selectedNickname() !== undefined)
  protected readonly conflictError = signal<string | null>(null)

  private readonly nickname = signal({
    nicknameValue: this.sessionStore.nickname()?.value() ?? '',
    rememberMe: this.sessionStore.nickname()?.remembered() ?? false,
  })

  protected nicknameForm = form(this.nickname, (path) => {
    required(path.nicknameValue, { message: 'Le pseudo est requis' })
    minLength(path.nicknameValue, 2, { message: 'Le pseudo doit faire au moins 2 caractères' })
    maxLength(path.nicknameValue, 50, { message: 'Le pseudo ne peut pas dépasser 50 caractères' })
  })

  constructor() {
    effect(() => {
      this.nickname()
      this.conflictError.set(null)
    })
  }

  protected readonly currentNicknameText = computed(() => {
    const fromForm = this.nickname().nicknameValue.trim()
    return fromForm || this.selectedNickname() || '?'
  })

  protected readonly avatarInitial = computed(() => {
    const name = this.currentNicknameText()
    if (name === '?') return '⚡'
    return name.slice(0, 2).toUpperCase()
  })

  protected readonly avatarGradientClass = computed(() => {
    const name = this.currentNicknameText()
    const gradients = [
      'from-rose-500 to-red-600 shadow-red-500/30',
      'from-blue-500 to-indigo-600 shadow-indigo-500/30',
      'from-amber-400 to-orange-500 shadow-amber-500/30',
      'from-emerald-400 to-green-600 shadow-emerald-500/30',
      'from-purple-500 to-indigo-600 shadow-purple-500/30',
      'from-pink-500 to-rose-600 shadow-pink-500/30',
      'from-cyan-400 to-blue-600 shadow-cyan-500/30',
    ]
    let hash = 0
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash)
    }
    const index = Math.abs(hash) % gradients.length
    return gradients[index]
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
            const errs = result.getErrors().get('nicknameValue') || result.getErrors().get('nickname')
            const msg = (errs && errs.length > 0)
              ? errs[0].message
              : 'Ce pseudo est déjà utilisé dans ce salon. Choisis un autre prénom ou ajoute une initiale.'
            this.conflictError.set(msg)
            return of(result.getErrorsForForm(this.nicknameForm))
          }
          if (result instanceof ForbidenError) {
            this.errorHandler.handleError(
              HandledFrontError.from(result),
            )

            this.message.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Vous devrez rejoindre une session avant de choisir un pseudo.',
              life: 5000,
            })
            return of(void 0)
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
