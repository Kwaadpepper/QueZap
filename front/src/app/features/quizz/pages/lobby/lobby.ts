import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { NicknameChooser } from '../../components/nickname-chooser/nickname-chooser'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-lobby',
  imports: [
    NicknameChooser,
  ],
  templateUrl: './lobby.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Lobby {
  private readonly sessionStore = inject(ActiveSessionStore)

  protected readonly nickname = computed(() => this.sessionStore.nickname()?.value())
  protected readonly participants = computed(() => this.sessionStore.participants())
}
