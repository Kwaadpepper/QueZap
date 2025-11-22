import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core'
import { Router } from '@angular/router'

import { Button } from 'primeng/button'

import { Config, LayoutSettings } from '@quezap/core/services'

import { ExitButton, NicknameChooser, ParticipantList } from '../../components'
import { SESSION_OBSERVER_SERVICE, SessionObserverMockService } from '../../services'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-lobby',
  imports: [
    NicknameChooser,
    ParticipantList,
    Button,
    ExitButton,
  ],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Lobby {
  private readonly router = inject(Router)
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)
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
    if (this.sessionStore.sessionHasStarted()) {
      this.navigateToQuizz()
    }

    effect(() => {
      if (this.sessionStore.sessionHasStarted()) {
        this.navigateToQuizz()
      }
    })

    effect((onCleanUp) => {
      this.layout.asWebsite.set(false)
      this.layout.inContainer.set(false)
      onCleanUp(() => {
        this.layout.asWebsite.set(true)
        this.layout.inContainer.set(true)
      })
    })

    if (this.sessionObserver instanceof SessionObserverMockService) {
      this.sessionObserver.mockParticipants()
    }
  }

  /** Debug function */
  protected onStartSession() {
    if (!(this.sessionObserver instanceof SessionObserverMockService)) {
      alert('Cette fonctionnalité n\'est disponible qu\'avec le service de mock')
      return
    }

    this.sessionObserver.mockSessionStart(this.sessionStore.session()!.code)
    this.sessionObserver.mockWaitingQuestion(this.sessionStore.session()!.code)
  }

  private navigateToQuizz() {
    this.router.navigateByUrl('/quizz', { skipLocationChange: true })
  }
}
