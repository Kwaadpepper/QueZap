import { ChangeDetectionStrategy, Component, inject } from '@angular/core'

import { SESSION_OBSERVER_SERVICE, SessionObserverMockService } from '../../services'

@Component({
  selector: 'quizz-quizz-runner',
  imports: [],
  templateUrl: './quizz-runner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizzRunner {
  /** Debug function */
  protected onNextQuestion() {
    const sessionObserver = inject(SESSION_OBSERVER_SERVICE)

    if (!(sessionObserver instanceof SessionObserverMockService)) {
      alert('Cette fonctionnalité n\'est disponible qu\'avec le service de mock')
      return
    }
    sessionObserver.mockNextQuestion()
  }
}
