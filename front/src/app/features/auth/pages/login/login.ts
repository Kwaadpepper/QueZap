import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Message } from 'primeng/message'
import { firstValueFrom } from 'rxjs'

import { ExternalValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services'
import { zod } from '@quezap/core/tools'
import { AuthenticatedUserStore } from '@quezap/shared/stores'

@Component({
  selector: 'quizz-login',
  imports: [
    Field,
    Button,
    InputText,
    Message,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements OnInit {
  private readonly config = inject(Config)
  private readonly authenticatedUserStore = inject(AuthenticatedUserStore)
  private readonly message = inject(MessageService)
  private readonly router = inject(Router)

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
  protected readonly invalidCredentials = signal(false)

  protected readonly loginForm = form(this.loginInfo, (path) => {
    validateStandardSchema(path, zod.object({
      email: zod.email('Email invalide'),
      password: zod.string().nonempty('Mot de passe requis'),
    }))
  })

  ngOnInit(): void {
    if (this.authenticatedUserStore.isLoggedIn()) {
      this.router.navigateByUrl(this.redirectUrl)
    }
  }

  protected onLogin() {
    if (this.loginForm().invalid()) {
      this.loginForm().markAsTouched()
      return
    }

    this.invalidCredentials.set(false)

    submit(this.loginForm, async (form) => {
      return new Promise((resolve, reject) => {
        firstValueFrom(
          this.authenticatedUserStore.login(
            form.email().value(),
            form.password().value(),
          ),
        ).then(() => {
          this.invalidCredentials.set(false)
          this.resetCredentials()
          resolve()
          this.message.add({
            severity: 'success',
            summary: 'Connexion réussie',
            closable: false,
            life: 2000,
          })
          this.router.navigateByUrl(this.redirectUrl)
        }).catch((err) => {
          if (err instanceof ExternalValidationError) {
            resolve(err.getErrorsForForm(form))
            this.invalidCredentials.set(true)
          }
          else {
            this.resetPassword()
            reject(err)
            this.message.add({
              severity: 'error',
              summary: 'Erreur lors de la connexion',
              life: 5000,
            })
          }
          return err
        })
      })
    })
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
