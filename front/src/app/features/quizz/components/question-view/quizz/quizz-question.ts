import {
  ChangeDetectionStrategy, Component, computed, effect, inject, input, model,
} from '@angular/core'
import { form, validateStandardSchema } from '@angular/forms/signals'

import * as zod from 'zod/v4'

import { PictureUrl, QuestionId, QuestionWithAnswers, QuizzQuestion } from '@quezap/domain/models'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import {
  createAnswerSubmissionState,
  Picture,
  PrintableAnswer,
  QuestionAlert,
  QuestionAnswer,
  SubmissionFeedback,
} from '../parts'

type Responses = Record<number, boolean>

@Component({
  selector: 'quizz-question-quizz',
  imports: [
    Picture,
    QuestionAnswer,
    QuestionAlert,
    SubmissionFeedback,
  ],
  templateUrl: './quizz-question.html',
  styles: [`
    :host {
      width: 100%;
      height: 100%;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizzQuestionView {
  private readonly timerStore = inject(TimerStore)
  private readonly sessionStore = inject(ActiveSessionStore)
  private readonly submission = createAnswerSubmissionState()

  protected readonly hasSubmitted = this.submission.hasSubmitted
  protected readonly isSubmitting = this.submission.isSubmitting
  protected readonly submissionError = this.submission.submissionError

  protected readonly isRevealing = computed<boolean>(() => this.sessionStore.lastReveal() !== undefined)
  protected readonly isSessionPaused = computed<boolean>(() => this.sessionStore.isSessionPaused())
  protected readonly timeLeft = computed<number | undefined>(() => this.timerStore.timeLeft())

  // Question related signals
  readonly question = input.required<QuizzQuestion & QuestionWithAnswers>()
  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)

  // Answers related signals
  protected readonly answers = computed<PrintableAnswer[]>(() => this.question().answers)
  protected readonly responses = model<Responses>({})

  protected readonly hasAnswered = computed<boolean>(() => this.responseForm().valid())
  protected readonly cannotAnswerAnymore = computed<boolean>(() =>
    this.isSessionPaused() || (this.timeLeft() === 0 && this.timeLeft() !== undefined),
  )

  protected readonly responseForm = form(this.responses, (path) => {
    validateStandardSchema(path, zod.record(
      zod.string(), zod.boolean())
      .refine(data => Object.keys(data).length >= 2 && Object.keys(data).length <= 4,
        'ERREUR: Le nombre de réponses doit être entre 2 et 4.')
      .refine(data => Object.values(data).some(Boolean),
        'Il faut choisir au moins une réponse correcte.'),
    )
  })

  private lastQuestionId: QuestionId | undefined

  constructor() {
    effect(() => {
      const q = this.question()
      if (q.id === this.lastQuestionId) {
        return
      }
      this.lastQuestionId = q.id

      const choice = this.sessionStore.mySubmittedChoice()
      const restoredChoice = choice?.questionId === q.id ? choice : undefined
      this.responses.set({
        ...Object.fromEntries(q.answers.map(answer => [
          answer.index,
          restoredChoice !== undefined && answer.index === restoredChoice.answerIndex,
        ])),
      })
      this.submission.resetForQuestion(restoredChoice !== undefined)
    })
  }

  protected selectAnswer(index: number) {
    if (this.cannotAnswerAnymore()) return

    const previousResponses = this.responses()
    const currentQuestion = this.question()
    const chosenAnswer = currentQuestion.answers.find(a => a.index === index)
    const choiceLabel = chosenAnswer?.value || `Choix ${index + 1}`

    this.responses.set(
      Object.fromEntries(currentQuestion.answers.map(a => [a.index, a.index === index])),
    )

    this.submission.submit({
      questionId: currentQuestion.id,
      answerIndex: index,
      choiceLabel,
      onRollback: () => {
        this.responses.set(previousResponses)
      },
    })
  }
}
