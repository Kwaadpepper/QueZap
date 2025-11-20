import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ConfirmationService } from 'primeng/api'
import { Button } from 'primeng/button'
import { ConfirmPopupModule } from 'primeng/confirmpopup'

@Component({
  selector: 'quizz-exit-button',
  imports: [
    Button,
    ConfirmPopupModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './exit-button.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExitButton {
  readonly #exitUrl = '/'
  private readonly router = inject(Router)
  private readonly confirmationService = inject(ConfirmationService)

  protected onQuitQuizz($event: Event): void {
    this.confirmationService.confirm({
      target: $event.currentTarget as EventTarget,
      accept: () => {
        this.router.navigateByUrl(this.#exitUrl)
      },
    })
  }
}
