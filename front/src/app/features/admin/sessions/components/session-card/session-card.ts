import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'
import { CardModule } from 'primeng/card'

import { Session } from '@quezap/domain/models'

@Component({
  selector: 'quizz-session-card',
  imports: [
    CardModule,
    Button,
  ],
  templateUrl: './session-card.html',
  styleUrl: './session-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionCard {
  private readonly router = inject(Router)

  readonly session = input.required<Session>()

  protected readonly isEnded = computed(() => this.session().status === 'ENDED' || this.session().endedAt !== null)
  protected readonly isLive = computed(() =>
    (this.session().status === 'STARTED' || this.session().startedAt !== null) && !this.isEnded(),
  )

  protected readonly isWaiting = computed(() => !this.isLive() && !this.isEnded())

  protected onSessionEdit(): void {
    this.router.navigate(['/admin/sessions', this.session().id, 'edit'])
  }

  protected onSessionPresent(): void {
    const url = this.router.serializeUrl(
      this.router.createUrlTree(['/projector', this.session().id]),
    )
    window.open(url, '_blank')
  }

  protected onSessionReport(): void {
    this.router.navigate(['/admin/sessions', this.session().id, 'report'])
  }
}
