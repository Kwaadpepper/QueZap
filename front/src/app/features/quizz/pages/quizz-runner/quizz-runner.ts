import {
  ChangeDetectionStrategy, Component, computed, effect, inject, signal,
} from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Message } from 'primeng/message'

import { Config, LayoutSettings } from '@quezap/core/services'
import {
  isBinaryQuestion, isBooleanQuestion, isQuizzQuestion, MixedQuestion, QuestionType, QuestionWithAnswers,
} from '@quezap/domain/models'
import { Spinner } from '@quezap/shared/components'

import { DebugToolbar, ExitButton } from '../../components'
import {
  BinaryQuestionView, BooleanQuestionView,
  QuestionTimer, QuizzQuestionView,
} from '../../components/question-view'
import { isNoMoreQuestions, isWaitingQuestion, NoMoreQuestions } from '../../services'
import { ActiveSessionStore, TimerStore } from '../../stores'

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
    QuestionTimer,
    ExitButton,
  ],
})
export class QuizzRunner {
  readonly #endedUrl = '/quizz/ended'
  private readonly router = inject(Router)
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)
  private readonly message = inject(MessageService)
  private readonly timerStore = inject(TimerStore)
  private readonly sessionStore = inject(ActiveSessionStore)

  protected readonly QuestionType = QuestionType
  protected readonly isBinary = isBinaryQuestion
  protected readonly isBoolean = isBooleanQuestion
  protected readonly isQuizz = isQuizzQuestion

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly question = signal<QuestionWithAnswers | undefined>(undefined)

  constructor() {
    effect((onCleanup) => {
      const question = this.sessionStore.question()
      if (!question) {
        return
      }
      if (isWaitingQuestion(question)) {
        this.question.set(undefined)
      }
      else {
        this.handleQuestion(question)
      }
      onCleanup(() => {
        this.timerStore.clearTimer()
      })
    })

    effect((onCleanUp) => {
      this.layout.asWebsite.set(false)
      this.layout.inContainer.set(false)
      onCleanUp(() => {
        this.layout.asWebsite.set(true)
        this.layout.inContainer.set(true)
      })
    })
  }

  protected reloadQuestion() {
    // FIXME: implement reload logic
    throw new Error('Method not implemented.')
  }

  // TODO: Implement behavior for when time is exhausted (e.g., mark question as unanswered, move to next question)
  protected onTimeExhausted() {
    throw new Error('Method not implemented.')
    console.log('Time exhausted for question', this.question()?.id)
  }

  private handleQuestion(question: MixedQuestion & QuestionWithAnswers | NoMoreQuestions) {
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
        this.timerStore.setTimer(question.limit)
        break
      default:
        throw new Error('Unknown question type')
    }
  }
}
