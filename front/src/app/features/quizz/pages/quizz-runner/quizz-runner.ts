import {
  ChangeDetectionStrategy, Component, computed, effect, inject, signal,
} from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Message } from 'primeng/message'

import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'
import { matchesTolerant } from '@quezap/core/services/text-normalizer/text-normalizer'
import {
  isBinaryQuestion,
  isBooleanQuestion,
  isCashQuestion,
  isQuizzQuestion,
  MixedQuestion,
  QuestionType,
  QuestionWithAnswers,
} from '@quezap/domain/models'
import { Spinner } from '@quezap/shared/components/spinner/spinner'

import { DebugToolbar } from '../../components/debug-toolbar/debug-toolbar'
import { ExitButton } from '../../components/exit-button/exit-button'
import {
  BinaryQuestionView, BooleanQuestionView, CashQuestionView,
  QuestionTimer, QuizzQuestionView,
} from '../../components/question-view'
import { QuestionIcon, QuestionIconType } from '../../components/question-view/parts/question-icon/question-icon'
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
    CashQuestionView,
    Message,
    Spinner,
    Button,
    QuestionTimer,
    ExitButton,
    QuestionIcon,
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
  protected readonly isCash = isCashQuestion

  protected readonly isDebug = computed(() => this.config.debug())
  protected readonly question = signal<QuestionWithAnswers | undefined>(undefined)
  protected readonly lastReveal = computed(() => this.sessionStore.lastReveal())
  protected readonly isSessionPaused = computed(() => this.sessionStore.isSessionPaused())
  protected readonly mySubmittedChoice = computed(() => this.sessionStore.mySubmittedChoice())
  protected readonly myRanking = computed(() => this.sessionStore.myRanking())
  protected readonly pointsEarned = computed(() => {
    const rank = this.myRanking()
    if (!rank) return 0
    return rank.delta !== undefined ? rank.delta : Math.max(0, rank.score - rank.previousScore)
  })

  protected getChoiceColorClass(index: number): string {
    const q = this.question()
    const isBool = q?.type === 'BOOLEAN' || (q?.answers?.length === 2 && q.answers[0].value === 'Vrai')
    if (isBool) {
      return index === 0 ? 'answer-option-3' : 'answer-option-0'
    }
    return `answer-option-${index % 4}`
  }

  protected getChoiceForm(index: number): QuestionIconType {
    const q = this.question()
    const isBool = q?.type === 'BOOLEAN' || (q?.answers?.length === 2 && q.answers[0].value === 'Vrai')
    if (isBool) {
      return index === 0 ? QuestionIconType.Circle : QuestionIconType.Cross
    }
    const forms = [
      QuestionIconType.Circle,
      QuestionIconType.Diamond,
      QuestionIconType.Cross,
      QuestionIconType.Hexagon,
    ]
    return forms[index % forms.length]
  }

  protected readonly liveAnnouncement = computed<string>(() => {
    if (this.isSessionPaused()) {
      return 'Session en pause. Écoutez les consignes du professeur.'
    }
    const reveal = this.lastReveal()
    if (reveal) {
      return 'Résultats révélés. Regardez le tableau pour les explications.'
    }
    const q = this.question()
    if (q) {
      return `Nouvelle question : ${q.value}`
    }
    return 'En attente de la prochaine question.'
  })

  constructor() {
    effect(() => {
      if (this.sessionStore.lastReveal() !== undefined) {
        this.timerStore.clearTimer()
      }
    })

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
    window.location.reload()
  }

  protected onTimeExhausted() {
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
      case QuestionType.Cash:
        this.question.set(question)
        this.timerStore.setTimer(question.limit)
        break
      default:
        throw new Error('Unknown question type')
    }
  }

  protected isChoiceCorrect(
    choice: { answerIndex: number, label: string },
    reveal: { correctIndices: number[], acceptedAnswers?: string[] },
  ): boolean {
    if (this.question()?.type === QuestionType.Cash) {
      if (!reveal.acceptedAnswers || reveal.acceptedAnswers.length === 0) {
        return false
      }
      return reveal.acceptedAnswers.some(ans => matchesTolerant(choice.label, ans))
    }
    return reveal.correctIndices.includes(choice.answerIndex)
  }
}
