import { Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { ImageModule } from 'primeng/image'

import { Debugbar } from './core/components'
import { ConfigService } from './core/services'

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
  private readonly config = inject(ConfigService)
  private readonly router = inject(Router)

  protected readonly asWebsite = signal(true)

  protected readonly footer = computed(() => {
    return {
      authorName: this.config.appConfig.value().authorName,
      authorEmail: this.config.appConfig.value().authorEmail,
    }
  })

  protected readonly onAdminPath = signal(false)

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.onAdminPath.set(this.router.url.startsWith('/admin'))
      }
    })
  }
}
