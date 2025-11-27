import { Component, input, output } from '@angular/core'

import { ButtonModule } from 'primeng/button'

import { IconFacade } from '../icon/icon-facade'

@Component({
  selector: 'quizz-scroll-top',
  imports: [
    ButtonModule,
    IconFacade,
  ],
  templateUrl: './scroll-top.html',
  styleUrl: './scroll-top.css',
})
export class ScrollTopComponent {
  readonly showButton = input.required<boolean>()
  readonly clicked = output<void>()

  protected onClick(): void {
    this.clicked.emit()
  }
}
