import { Component, inject, model, signal } from '@angular/core'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Dialog } from 'primeng/dialog'
import { InputText } from 'primeng/inputtext'
import { firstValueFrom } from 'rxjs'

import { FieldError } from '@quezap/core/directives'
import { ExternalValidationError } from '@quezap/core/errors'
import { zod } from '@quezap/core/tools'

import { REGISTER_SERVICE, RegisterMockService } from '../../services'

@Component({
  selector: 'quizz-register-modal',
  imports: [
    Dialog,
    InputText,
    Button,
    Field,
    FieldError,
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

  private readonly userInfo = signal({
    email: '',
    username: '',
  })

  protected readonly registerForm = form(this.userInfo, (path) => {
    validateStandardSchema(path, zod.object({
      email: zod.email('Email invalide'),
      username: zod.string()
        .min(3, '3 caractères minimum')
        .max(20, '20 caractères maximum'),
    }))
  })

  public readonly visible = model(false)

  protected onModalHide() {
    this.userInfo.set({
      email: '',
      username: '',
    })
    this.registerForm().reset()
  }

  protected onRegister() {
    if (this.registerForm().invalid()) {
      return
    }

    submit(this.registerForm, async (form) => {
      return new Promise((resolve, reject) => {
        firstValueFrom(this.registerService.register(
          form.email().value(),
          form.username().value(),
        )).then(() => {
          this.message.add({
            severity: 'success',
            summary: 'Inscription réussie',
            detail: 'Vous allez recevoir un email contenant votre lien d\'activation',
            sticky: true,
          })
          resolve()
          this.visible.set(false)
        }).catch((err) => {
          if (err instanceof ExternalValidationError) {
            resolve(err.getErrorsForForm(form))
          }
          else {
            reject(err)
            this.message.add({
              severity: 'error',
              summary: 'Erreur lors de l\'inscription',
              detail: 'Un erreur est survenu lors de l\'envoi du formulaire',
              life: 5000,
            })
          }
          return err
        })
      })
    })
  }

  protected onCancel() {
    this.visible.set(false)
  }
}
