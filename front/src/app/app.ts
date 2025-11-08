import { Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { ImageModule } from 'primeng/image'

import { Config, LayoutSettings } from './core/services'
import { Debugbar, LoadingBar } from './shared/components'

@Component({
  selector: 'quizz-root',
  imports: [
    RouterOutlet,
    RouterModule,
    ButtonDirective,
    ImageModule,
    Debugbar,
    LoadingBar,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly config = inject(Config)
  private readonly router = inject(Router)
  protected readonly layout = inject(LayoutSettings)

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
