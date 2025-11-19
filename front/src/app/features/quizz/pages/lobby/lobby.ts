import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'

import { Config } from '@quezap/core/services'

import { NicknameChooser, ParticipantList } from '../../components'
import { SESSION_OBSERVER_SERVICE, SessionObserverMockService } from '../../services'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-lobby',
  imports: [
    NicknameChooser,
    ParticipantList,
    Button,
  ],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Lobby {
  private readonly router = inject(Router)
  private readonly config = inject(Config)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly sessionObserver = inject(SESSION_OBSERVER_SERVICE)

  protected readonly isDebug = computed(() => this.config.debug())

  private readonly nickname = computed(() => this.sessionStore.nickname()?.value())

  private readonly hasNickname = computed(() => {
    const name = this.nickname()
    return name !== undefined && name !== ''
  })

  protected readonly readyTonJoinQuizz = computed(() => this.hasNickname())

  constructor() {
    effect(() => {
      if (this.sessionStore.sessionIsRunning()) {
        this.navigateToQuizz()
      }
    })
  }

  /** Debug function */
  protected onStartSession() {
    if (!(this.sessionObserver instanceof SessionObserverMockService)) {
      alert('Cette fonctionnalité n\'est disponible qu\'avec le service de mock')
      return
    }

    this.sessionObserver.mockNextQuestion()
  }

  private navigateToQuizz() {
    this.router.navigateByUrl('/quizz', {
      skipLocationChange: true,
    })
  }
}
