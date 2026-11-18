import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core'

import { CashQuestion, PictureUrl, QuestionId, QuestionWithAnswers } from '@quezap/domain/models'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { Picture } from '../parts/picture/picture'
import { QuestionAlert } from '../parts/question-alert/question-alert'
import { createAnswerSubmissionState } from '../parts/question-submission'
import { SubmissionFeedback } from '../parts/submission-feedback/submission-feedback'

@Component({
  selector: 'quizz-question-cash',
  imports: [
    Picture,
    QuestionAlert,
    SubmissionFeedback,
  ],
  templateUrl: './cash-question.html',
  styles: [`
    :host {
      width: 100%;
      height: 100%;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CashQuestionView {
  private readonly timerStore = inject(TimerStore)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly submission = createAnswerSubmissionState()

  readonly question = input.required<CashQuestion & QuestionWithAnswers>()

  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)

  protected readonly isRevealing = computed<boolean>(() => this.sessionStore.lastReveal() !== undefined)
  protected readonly isSessionPaused = computed<boolean>(() => this.sessionStore.isSessionPaused())
  protected readonly timeLeft = computed<number | undefined>(() => this.timerStore.timeLeft())
  protected readonly cannotAnswerAnymore = computed<boolean>(
    () => this.isSessionPaused() || (this.timeLeft() === 0 && this.timeLeft() !== undefined),
  )

  protected readonly textInput = signal<string>('')
  protected readonly hasSubmitted = this.submission.hasSubmitted
  protected readonly isSubmitting = this.submission.isSubmitting
  protected readonly submissionError = this.submission.submissionError
  protected readonly lastSubmittedText = signal<string>('')
  protected readonly submittedLabel = computed<string>(() => `Réponse envoyée : « ${this.lastSubmittedText()} »`)

  private lastQuestionId: QuestionId | undefined

  constructor() {
    effect(() => {
      const currentId = this.question().id
      if (currentId === this.lastQuestionId) {
        return
      }
      this.lastQuestionId = currentId

      const choice = this.sessionStore.mySubmittedChoice()
      const restoredChoice = choice?.questionId === currentId ? choice : undefined
      if (restoredChoice?.label) {
        this.textInput.set(restoredChoice.label)
        this.lastSubmittedText.set(restoredChoice.label)
        this.submission.resetForQuestion(true)
      }
      else {
        this.textInput.set('')
        this.lastSubmittedText.set('')
        this.submission.resetForQuestion(false)
      }
    })
  }

  protected onTextInput(event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.textInput.set(event.target.value)
    }
  }

  protected submitAnswer(): void {
    if (this.cannotAnswerAnymore()) {
      return
    }
    const trimmed = this.textInput().trim()
    if (!trimmed) {
      return
    }

    const previousSubmittedText = this.lastSubmittedText()
    const currentId = this.question().id

    this.submission.submit({
      questionId: currentId,
      answerIndex: 0,
      textAnswer: trimmed,
      choiceLabel: trimmed,
      onSuccess: () => {
        this.lastSubmittedText.set(trimmed)
      },
      onRollback: () => {
        this.lastSubmittedText.set(previousSubmittedText)
      },
    })
  }
}
