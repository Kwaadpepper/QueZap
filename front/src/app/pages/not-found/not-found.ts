import { Component, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonModule } from 'primeng/button'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

@Component({
  selector: 'quizz-not-found',
  imports: [
    ButtonModule,
    IconFacade,
  ],
  templateUrl: './not-found.html',
})
export class NotFound {
  private readonly router = inject(Router)
}
