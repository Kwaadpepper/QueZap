import { Component, ErrorHandler, inject, OnInit, signal } from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Message } from 'primeng/message'
import { ProgressSpinnerModule } from 'primeng/progressspinner'
import { catchError, firstValueFrom, of, tap } from 'rxjs'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { isSuccess } from '@quezap/core/types'
import { Spinner } from '@quezap/shared/components'

import { ACCOUNT_ACTIVATION_SERVICE } from '../services'

@Component({
  selector: 'quizz-account-activation',
  imports: [
    ProgressSpinnerModule,
    Message,
    Spinner,
  ],
  templateUrl: './account-activation.html',
})
export class AccountActivation implements OnInit {
  private readonly router = inject(Router)
  private readonly message = inject(MessageService)
  private readonly activationService = inject(ACCOUNT_ACTIVATION_SERVICE)
  private readonly errorHandler = inject(ErrorHandler)

  protected readonly errorOccurred = signal(false)

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

    firstValueFrom(
      this.activationService.activate(token).pipe(
        tap((result) => {
          if (isSuccess(result)) {
            this.message.add({
              severity: 'success',
              summary: 'Compte activé',
              detail: 'Votre compte a bien été activé, vous pouvez maintenant vous connecter.',
            })
            this.router.navigateByUrl('/auth/login')
            return
          }

          if (result.error instanceof ValidationError) {
            this.message.add({
              severity: 'error',
              summary: 'Échec de l\'activation',
              detail: 'Le lien d\'activation est invalide ou a expiré.',
              sticky: true,
            })
            this.router.navigateByUrl('/')
            return
          }

          throw HandledFrontError.from(
            new Error('Activation failed with unexpected error', { cause: result.error }),
          )
        }),
        catchError((err) => {
          this.errorOccurred.set(true)

          this.errorHandler.handleError(
            HandledFrontError.from(err),
          )

          return of(void 0)
        }),
      ),
    )
  }
}
