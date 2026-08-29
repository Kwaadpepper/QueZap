import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { Button, ButtonIcon } from 'primeng/button'
import { CardModule } from 'primeng/card'

import { Session } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

@Component({
  selector: 'quizz-session-card',
  imports: [
    CardModule,
    Button,
    ButtonIcon,
    IconFacade,
  ],
  templateUrl: './session-card.html',
  styleUrl: './session-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionCard {
  private readonly router = inject(Router)

  readonly session = input.required<Session>()

  protected readonly isEnded = computed(() => this.session().endedAt !== null)
  protected readonly isLive = computed(() => this.session().startedAt !== null && !this.isEnded())
  protected readonly isWaiting = computed(() => !this.isLive() && !this.isEnded())

  protected onSessionEdit(): void {
    this.router.navigate(['/admin/sessions', this.session().id, 'edit'])
  }

  protected onSessionPresent(): void {
    this.router.navigate(['/admin/sessions', this.session().id, 'present'])
  }
}
