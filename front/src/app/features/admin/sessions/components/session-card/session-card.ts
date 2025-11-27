import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { CardModule } from 'primeng/card'

import { Session } from '@quezap/domain/models'

@Component({
  selector: 'quizz-session-card',
  imports: [
    CardModule,
  ],
  templateUrl: './session-card.html',
  styleUrl: './session-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionCard {
  private readonly router = inject(Router)

  readonly session = input.required<Session>()

  protected onSessionEdit(): void {
    this.router.navigate(['/admin/sessions', this.session().id, 'edit'])
  }
}
