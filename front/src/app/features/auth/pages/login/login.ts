import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'
import { Router, RouterModule } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Message } from 'primeng/message'
import { catchError, firstValueFrom, of, tap, throwError } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services'
import { zod } from '@quezap/core/tools'
import { AuthenticatedUserStore } from '@quezap/shared/stores'

@Component({
  selector: 'quizz-login',
  imports: [
    RouterModule,
    Field,
    Button,
    InputText,
    Message,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly config = inject(Config)
  private readonly authenticatedUserStore = inject(AuthenticatedUserStore)
  private readonly message = inject(MessageService)
  private readonly router = inject(Router)
  private readonly destroyRef = inject(DestroyRef)

  private readonly redirectUrl = '/admin'

  readonly #mockedCredentials = {
    email: 'user@example.net',
    password: 'password',
  }

  private readonly loginInfo = signal({
    email: '',
    password: '',
  })

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly loginError = signal(false)
  protected readonly invalidCredentials = signal(false)

  protected readonly loginForm = form(this.loginInfo, (path) => {
    validateStandardSchema(path, zod.object({
      email: zod.email('Email invalide'),
      password: zod.string().nonempty('Mot de passe requis'),
    }))
  })

  protected onLogin() {
    if (this.loginForm().invalid()) {
      this.loginForm().markAsTouched()
      return
    }

    this.loginError.set(false)
    this.invalidCredentials.set(false)

    submit(this.loginForm, async form =>
      firstValueFrom(
        this.authenticatedUserStore.login(
          form.email().value(),
          form.password().value(),
        ).pipe(
          takeUntilDestroyed(this.destroyRef),
          tap(() => {
            this.invalidCredentials.set(false)
            this.resetCredentials()
            this.message.add({
              severity: 'success',
              summary: 'Connexion réussie',
              closable: false,
              life: 2000,
            })
            this.router.navigateByUrl(this.redirectUrl)
          }),
          catchError((err) => {
            if (err instanceof ValidationError) {
              this.invalidCredentials.set(true)
              return of(err.getErrorsForForm(form))
            }

            this.resetPassword()
            this.loginError.set(true)

            return throwError(() => err)
          }),
        ),
      ),
    )
  }

  protected onFillMockedCredentials() {
    this.loginInfo.set({
      ...this.#mockedCredentials,
    })
    this.loginForm().markAsDirty()
  }

  private resetPassword() {
    this.loginInfo.set({
      ...this.loginInfo(),
      password: '',
    })
    this.loginForm().markAsDirty()
  }

  private resetCredentials() {
    this.loginInfo.set({
      email: '',
      password: '',
    })
    this.loginForm().markAsDirty()
  }
}
