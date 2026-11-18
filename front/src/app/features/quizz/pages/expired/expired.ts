import { ChangeDetectionStrategy, Component, inject } from '@angular/core'

import { JoinForm } from '../../components/join-form/join-form'
import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'

@Component({
  selector: 'quizz-expired',
  imports: [JoinForm],
  templateUrl: './expired.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Expired {
  private readonly persistence = inject(ActiveSessionPersistence)

  constructor() {
    this.persistence.clearKeepingNickname()
  }
}
