import { Component, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Divider } from 'primeng/divider'
import { ImageModule } from 'primeng/image'
import { Toast } from 'primeng/toast'

import { LayoutSettings } from './core/services'
import { Footer } from './layout/footer/footer'
import { AdminNav, SiteNav } from './layout/navigation'
import { Debugbar, LoadingBar } from './shared/components'

@Component({
  selector: 'quizz-root',
  imports: [
    RouterOutlet,
    RouterModule,
    ImageModule,
    Debugbar,
    LoadingBar,
    Toast,
    AdminNav,
    SiteNav,
    Divider,
    Footer,
  ],
  providers: [MessageService],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router)
  protected readonly layout = inject(LayoutSettings)

  protected readonly onAdminPath = signal(false)

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.onAdminPath.set(this.router.url.startsWith('/admin'))
      }
    })
  }
}
