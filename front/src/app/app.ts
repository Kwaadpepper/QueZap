import { Component, computed, inject, signal } from '@angular/core'
import { RouterModule, RouterOutlet } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { ImageModule } from 'primeng/image'

import { Debugbar } from './core/components/debugbar/debugbar'
import { ConfigService } from './core/services/config'

@Component({
  selector: 'quizz-root',
  imports: [
    RouterOutlet,
    RouterModule,
    ButtonDirective,
    ImageModule,
    Debugbar,
  ],
  templateUrl: './app.html',
})
export class App {
  public readonly asWebsite = signal(true)
  private readonly config = inject(ConfigService)

  public readonly footer = computed(() => {
    return {
      authorName: this.config.appConfig.value().authorName,
      authorEmail: this.config.appConfig.value().authorEmail,
    }
  })
}
