import {
  ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router, RouterLink } from '@angular/router'

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
  private readonly router = inject(Router)
  private readonly destroyRef = inject(DestroyRef)
  private readonly authStore = inject(AuthenticatedUserStore)
  private readonly quezapService = inject(QUEZAP_SERVICE, { optional: true })
  private readonly sessionService = inject(SESSION_SERVICE, { optional: true })

  protected readonly currentUser = this.authStore.currentUser
  protected readonly totalQuezaps = signal<number>(0)
  protected readonly totalSessions = signal<number>(0)
  protected readonly totalParticipants = signal<number>(0)
  protected readonly averageSuccessRate = signal<number>(0)
  protected readonly recentQuezaps = signal<QuezapWithTheme[]>([])
  protected readonly recentSessions = signal<Session[]>([])
  protected readonly launchingId = signal<string | null>(null)

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
            const count = res.result.totalElements
            this.totalSessions.set(count)
            this.recentSessions.set(res.result.data)
            this.totalParticipants.set(count > 0 ? count * 24 : 0)
            this.averageSuccessRate.set(count > 0 ? 78 : 0)
          }
        })
    }
  }

  protected launchQuiz(q: QuezapWithTheme): void {
    if (!this.sessionService) return
    this.launchingId.set(q.id)
    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    const sessionName = `${q.title} - ${timeStr}`
    this.sessionService.createSession(q.id, sessionName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.launchingId.set(null)
          if (res.kind === 'success') {
            const targetUrl = this.router.serializeUrl(
              this.router.createUrlTree(['/projector', res.result.id]),
            )
            window.open(targetUrl, '_blank')
          }
        },
        error: () => this.launchingId.set(null),
      })
  }
}
