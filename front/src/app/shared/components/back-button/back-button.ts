import { Component, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'

import { IconFacade } from '../icon/icon-facade'

@Component({
  selector: 'quizz-back-button',
  imports: [Button, IconFacade],
  templateUrl: './back-button.html',
  styleUrl: './back-button.css',
})
export class BackButton {
  private readonly router = inject(Router)

  public readonly backUrl = input.required<string>()

  protected onGoBackLink(): void {
    this.router.navigateByUrl(this.backUrl())
  }
}
