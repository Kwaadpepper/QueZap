import {
  ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { RouterLink } from '@angular/router'

import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { ChipModule } from 'primeng/chip'

import { QuezapWithTheme, Session } from '@quezap/domain/models'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { QUEZAP_SERVICE } from '../../quezaps/services/quezap'
import { SESSION_SERVICE } from '../../sessions/services/session'

@Component({
  selector: 'quizz-home',
  imports: [
    RouterLink,
    ButtonModule,
    CardModule,
    ChipModule,
  ],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home implements OnInit {
  private readonly destroyRef = inject(DestroyRef)
  private readonly authStore = inject(AuthenticatedUserStore)
  private readonly quezapService = inject(QUEZAP_SERVICE, { optional: true })
  private readonly sessionService = inject(SESSION_SERVICE, { optional: true })

  protected readonly currentUser = this.authStore.currentUser
  protected readonly totalQuezaps = signal<number>(0)
  protected readonly totalSessions = signal<number>(0)
  protected readonly recentQuezaps = signal<QuezapWithTheme[]>([])
  protected readonly recentSessions = signal<Session[]>([])

  ngOnInit(): void {
    if (this.quezapService) {
      this.quezapService.getQuezapPage({ page: 1, pageSize: 4 })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((res) => {
          if (res.kind === 'success') {
            this.totalQuezaps.set(res.result.totalElements)
            this.recentQuezaps.set(res.result.data)
          }
        })
    }

    if (this.sessionService) {
      this.sessionService.getSessionPage({ page: 1, pageSize: 4 })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((res) => {
          if (res.kind === 'success') {
            this.totalSessions.set(res.result.totalElements)
            this.recentSessions.set(res.result.data)
          }
        })
    }
  }
}
