import { Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NavigationCancel, NavigationEnd, NavigationStart, Router, RouterModule, RouterOutlet } from '@angular/router'

import { Button } from 'primeng/button'
import { Divider } from 'primeng/divider'
import { Drawer } from 'primeng/drawer'
import { ImageModule } from 'primeng/image'
import { Toast } from 'primeng/toast'

import { LoadingStatus } from '@quezap/core/services'

import { LayoutSettings } from './core/services'
import { LogoutButton } from './features/auth/components'
import { Footer } from './layout/footer/footer'
import { AdminNav, SiteNav } from './layout/navigation'
import { Debugbar, LoadingBar, ScrollTopComponent } from './shared/components'
import { AuthenticatedUserStore } from './shared/stores'

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
    Drawer,
    Button,
    LogoutButton,
    ScrollTopComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router)
  private readonly LoadingStatus = inject(LoadingStatus)
  private readonly authenticatedUser = inject(AuthenticatedUserStore)
  protected readonly layout = inject(LayoutSettings)

  protected readonly drawerVisible = signal(false)
  protected readonly onAdminPath = signal(false)
  protected readonly isLoggedIn = computed(() => this.authenticatedUser.isLoggedIn())

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.LoadingStatus.start()
      }
      if (event instanceof NavigationEnd) {
        this.drawerVisible.set(false)
        this.LoadingStatus.stop()
        this.onAdminPath.set(this.router.url.startsWith('/admin'))
      }
      if (event instanceof NavigationCancel) {
        this.LoadingStatus.stop()
      }
    })
  }

  protected toggleDrawer() {
    this.drawerVisible.update(v => !v)
  }
}
