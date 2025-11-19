import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'

@Component({
  selector: 'quizz-exit-button',
  imports: [Button],
  templateUrl: './exit-button.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExitButton {
  readonly #exitUrl = '/'
  private readonly router = inject(Router)

  protected onQuitQuizz() {
    this.router.navigateByUrl(this.#exitUrl)
  }
}
