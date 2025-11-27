import {
  ChangeDetectionStrategy, Component, computed,
  DestroyRef,
  ElementRef,
  inject, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import {
  NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterModule, RouterOutlet,
} from '@angular/router'

import { ButtonModule } from 'primeng/button'
import { DividerModule } from 'primeng/divider'
import { DrawerModule } from 'primeng/drawer'
import { FocusTrapModule } from 'primeng/focustrap'
import { ImageModule } from 'primeng/image'
import { Toast } from 'primeng/toast'

import { LoadingStatus } from '@quezap/core/services/loading/loading-status'

import { LayoutSettings } from './core/services/layout/layout-settings'
import { LogoutButton } from './features/auth/components/logout-button/logout-button'
import { Footer } from './layout/footer/footer'
import { AdminNav, SiteNav } from './layout/navigation'
import { Debugbar } from './shared/components/debugbar/debugbar'
import { IconFacade } from './shared/components/icon/icon-facade'
import { LoadingBar } from './shared/components/loading-bar/loading-bar'
import { ScrollTopComponent } from './shared/components/scroll-top/scroll-top'
import { AuthenticatedUserStore } from './shared/stores/authenticated-user'

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
    DividerModule,
    Footer,
    DrawerModule,
    ButtonModule,
    LogoutButton,
    ScrollTopComponent,
    FocusTrapModule,
    IconFacade,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(scroll)': 'onScroll($event)' },
})
export class App {
  private readonly router = inject(Router)
  private readonly LoadingStatus = inject(LoadingStatus)
  private readonly authenticatedUser = inject(AuthenticatedUserStore)
  private readonly destroyRef = inject(DestroyRef)
  protected readonly layout = inject(LayoutSettings)

  protected readonly drawerVisible = signal(false)
  protected readonly isLoggedIn = computed(() => this.authenticatedUser.isLoggedIn())

  protected readonly inContainer = computed(() => this.layout.inContainer())
  protected readonly asWebsite = computed(() => this.layout.asWebsite())

  protected readonly onAdminPath = signal(false)
  protected readonly hideHeaderOnScroll = signal(false)

  readonly hostElement: HTMLElement = inject(ElementRef).nativeElement

  private lastScrollTop = 0
  protected readonly showScrollTopButton = computed(() => {
    return this.hideHeaderOnScroll()
  })

  constructor() {
    // * Update layout settings based on current path
    this.router.events.pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.LoadingStatus.start()
      }
      if (event instanceof NavigationEnd) {
        this.drawerVisible.set(false)
        this.LoadingStatus.stop()

        this.onAdminPath.set(event.url.startsWith('/admin'))
      }
      if (
        event instanceof NavigationCancel
        || event instanceof NavigationError
      ) {
        this.LoadingStatus.stop()
      }
    })
  }

  protected toggleDrawer() {
    this.drawerVisible.update(v => !v)
  }

  protected onScrollTopClick(): void {
    this.hostElement.scrollTo({ top: 0, behavior: 'smooth' })
  }

  protected onScroll(event: Event) {
    this.hideHeaderOnScroll.update(() => {
      const scrollTop = (event.target as HTMLElement).scrollTop
      const isScrollingDown = scrollTop > this.lastScrollTop

      this.lastScrollTop = Math.max(0, scrollTop)

      return isScrollingDown
    })
  }
}
