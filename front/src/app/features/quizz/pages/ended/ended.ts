import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { JoinForm } from '../../components/join-form/join-form'
import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'

@Component({
  selector: 'quizz-ended',
  imports: [
    JoinForm,
  ],
  templateUrl: './ended.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ended {
  private readonly persistence = inject(ActiveSessionPersistence)

  protected readonly nickname = computed(() =>
    this.persistence.getParticipantName() || this.persistence.retrieve()?.nickname || 'Joueur',
  )
}
