import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import { Config } from '@quezap/core/services'
import { Question, QuestionType } from '@quezap/domain/models'

import { SESSION_OBSERVER_SERVICE, SessionObserverMockService } from '../../services'
import { ActiveSessionStore } from '../../stores'

@Component({
  selector: 'quizz-quizz-runner',
  imports: [],
  templateUrl: './quizz-runner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizzRunner {
  private readonly config = inject(Config)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly sessionObserver = inject(SESSION_OBSERVER_SERVICE)

  protected readonly QuestionType = QuestionType

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly question = signal<Question | null>(null)

  constructor() {
    this.sessionObserver.questions().pipe(
      takeUntilDestroyed(),
    ).subscribe((questions) => {
      console.log('Questions updated:', questions)
    })
  }

  /** Debug function */
  protected onNextQuestion() {
    if (!(this.sessionObserver instanceof SessionObserverMockService)) {
      alert('Cette fonctionnalité n\'est disponible qu\'avec le service de mock')
      return
    }

    this.sessionObserver.mockNextQuestion(
      this.sessionStore.session()!.code,
    )
  }
}
