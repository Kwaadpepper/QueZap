import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Message } from 'primeng/message'
import { catchError, map, of, retry, switchMap, throwError } from 'rxjs'

import { Config } from '@quezap/core/services'
import { isFailure } from '@quezap/core/types'
import { isBinaryQuestion, isBooleanQuestion, isQuizzQuestion, MixedQuestion, Question, QuestionType } from '@quezap/domain/models'
import { Spinner } from '@quezap/shared/components'

import { DebugToolbar } from '../../components'
import { BinaryQuestionView, BooleanQuestionView, QuizzQuestionView } from '../../components/question-view'
import { isNoMoreQuestions, NoMoreQuestions, SESSION_OBSERVER_SERVICE } from '../../services'

@Component({
  selector: 'quizz-quizz-runner',
  templateUrl: './quizz-runner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DebugToolbar,
    BinaryQuestionView,
    BooleanQuestionView,
    QuizzQuestionView,
    Message,
    Spinner,
    Button,
  ],
})
export class QuizzRunner {
  readonly #endedUrl = '/quizz/ended'
  private readonly router = inject(Router)
  private readonly config = inject(Config)
  private readonly message = inject(MessageService)
  private readonly sessionObserver = inject(SESSION_OBSERVER_SERVICE)

  protected readonly QuestionType = QuestionType

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly question = signal<Question | null>(null)

  protected readonly isBinary = isBinaryQuestion
  protected readonly isBoolean = isBooleanQuestion
  protected readonly isQuizz = isQuizzQuestion

  constructor() {
    this.sessionObserver.questions().pipe(
      takeUntilDestroyed(),
      switchMap((response) => {
        return isFailure(response)
          ? throwError(() => response.error)
          : of(response.result)
      }),
      retry({ count: 3, delay: 1000, resetOnSuccess: true }),
      map(question => this.handleQuestion(question)),
      catchError((error) => {
        this.message.add({
          severity: 'error',
          summary: 'Erreur de chargement',
          detail: `Impossible de charger la question : ${error.message}`,
          sticky: true,
        })
        return []
      }),
    ).subscribe()
  }

  protected reloadQuestion() {
    // FIXME: implement reload logic
    throw new Error('Method not implemented.')
  }

  private handleQuestion(question: MixedQuestion | NoMoreQuestions) {
    if (isNoMoreQuestions(question)) {
      this.message.add({
        severity: 'info',
        summary: 'Fin du quizz',
        detail: 'Le quizz est terminé, bravo à tous les participants !',
      })
      this.router.navigate([this.#endedUrl])
      return
    }

    switch (question.type) {
      case QuestionType.Boolean:
      case QuestionType.Binary:
      case QuestionType.Quizz:
        this.question.set(question)
        break
      default:
        throw new Error('Unknown question type')
    }
  }
}
