import { Component, HostListener, signal } from '@angular/core'

import { ButtonModule } from 'primeng/button'

@Component({
  selector: 'quizz-scroll-top',
  imports: [
    ButtonModule,
  ],
  templateUrl: './scroll-top.html',
  styleUrl: './scroll-top.css',
})
export class ScrollTopComponent {
  readonly showButton = signal(false)

  onScrollTopClick(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  @HostListener('window:scroll')
  onScroll(): void {
    if (window.scrollY > 250) {
      this.showButton.set(true)
      return
    }

    if (this.showButton()) {
      this.showButton.set(false)
    }
  }
}
