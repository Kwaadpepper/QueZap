import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { ParticipantIcon } from '@quezap/shared/components/participant-icon/participant-icon'

import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-participant-list',
  imports: [ParticipantIcon],
  templateUrl: './participant-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ParticipantList {
  private readonly sessionStore = inject(ActiveSessionStore)

  protected readonly participants = computed(() => this.sessionStore.participants())
}
