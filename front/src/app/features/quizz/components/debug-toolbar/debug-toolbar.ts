import { ChangeDetectionStrategy, Component, inject } from '@angular/core'

import { Button } from 'primeng/button'
import { ButtonGroup } from 'primeng/buttongroup'

import { SESSION_OBSERVER_SERVICE, SessionObserverMockService } from '../../services'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-debug-toolbar',
  imports: [ButtonGroup, Button],
  templateUrl: './debug-toolbar.html',
  styleUrl: './debug-toolbar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DebugToolbar {
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly sessionObserver = inject(SESSION_OBSERVER_SERVICE)

  protected onMockAction(action: 'nextQuestion'
    | 'booleanQuestion'
    | 'binaryQuestion'
    | 'quizzQuestion'
    | 'mockNoMoreQuestions',
  ): void {
    if (!(this.sessionObserver instanceof SessionObserverMockService)) {
      alert('This feature is only available in mock mode.')
      return
    }

    const sessionCode = this.sessionStore.session()!.code

    console.debug('Mock action triggered:', action, 'for session', sessionCode)

    switch (action) {
      case 'nextQuestion': this.sessionObserver.mockNextQuestion(sessionCode)
        break
      case 'booleanQuestion': this.sessionObserver.mockBooleanQuestion(sessionCode)
        break
      case 'binaryQuestion': this.sessionObserver.mockBinaryQuestion(sessionCode)
        break
      case 'quizzQuestion': this.sessionObserver.mockQuizzQuestion(sessionCode)
        break
      case 'mockNoMoreQuestions': this.sessionObserver.mockNoMoreQuestions(sessionCode)
        break
      default: throw new Error(`Unknown mock action: ${action}`)
    }
  }
}
