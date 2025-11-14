import { Component, inject } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonModule } from 'primeng/button'

@Component({
  selector: 'quizz-not-found',
  imports: [
    ButtonModule,
  ],
  templateUrl: './not-found.html',
})
export class NotFound {
  private readonly router = inject(Router)
}
