import { Component, inject, OnInit } from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { ProgressSpinnerModule } from 'primeng/progressspinner'
import { firstValueFrom } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'

import { ACCOUNT_ACTIVATION_SERVICE, AccountActivationMockService } from '../services'

@Component({
  selector: 'quizz-account-activation',
  imports: [
    ProgressSpinnerModule,
  ],
  providers: [
    {
      provide: ACCOUNT_ACTIVATION_SERVICE,
      useClass: AccountActivationMockService,
    },
  ],
  templateUrl: './account-activation.html',
  styleUrl: './account-activation.css',
})
export class AccountActivation implements OnInit {
  private readonly router = inject(Router)
  private readonly message = inject(MessageService)
  private readonly activationService = inject(ACCOUNT_ACTIVATION_SERVICE)

  readonly #activationToken: string | null = null

  constructor() {
    const query = this.router.currentNavigation()?.initialUrl.queryParamMap ?? null

    this.#activationToken = query?.get('token') ?? null
  }

  ngOnInit() {
    const token = this.#activationToken
    if (token === null) {
      this.message.add({
        severity: 'info',
        summary: 'Vous avez été redirigé',
      })
      this.router.navigateByUrl('/')
      return
    }

    firstValueFrom(this.activationService.activate(token))
      .then(() => {
        this.message.add({
          severity: 'success',
          summary: 'Compte activé',
          detail: 'Votre compte a bien été activé, vous pouvez maintenant vous connecter.',
        })
        this.router.navigateByUrl('/auth/login')
      })
      .catch((error) => {
        if (error instanceof ValidationError) {
          this.message.add({
            severity: 'error',
            summary: 'Échec de l\'activation',
            detail: 'Le lien d\'activation est invalide ou a expiré.',
            sticky: true,
          })
          this.router.navigateByUrl('/')
          return
        }
        this.message.add({
          severity: 'error',
          summary: 'Erreur inconnue',
          detail: 'Une erreur inconnue est survenue lors de l\'activation de votre compte. Veuillez réessayer plus tard.',
          sticky: true,
        })
      })
  }
}
