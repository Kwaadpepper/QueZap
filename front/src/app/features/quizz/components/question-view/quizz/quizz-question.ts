import {
  ChangeDetectionStrategy, Component, computed, effect, inject, input, model,
} from '@angular/core'
import { form, validateStandardSchema } from '@angular/forms/signals'

import { zod } from '@quezap/core/tools'
import { PictureUrl, QuestionId, QuestionWithAnswers, QuizzQuestion } from '@quezap/domain/models'
import { TimerStore } from '@quezap/features/quizz/stores'

import { Picture, PrintableAnswer, QuestionAnswer } from '../parts'
import { QuestionAlert } from '../parts/question-alert/question-alert'

type Responses = Record<number, boolean>

@Component({
  selector: 'quizz-question-quizz',
  imports: [
    Picture,
    QuestionAnswer,
    QuestionAlert,
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
  protected readonly cannotAnswerAnymore = computed<boolean>(() => this.timeLeft() === 0
    && this.timeLeft() !== undefined)

  protected readonly responseForm = form(this.responses, (path) => {
    validateStandardSchema(path, zod.record(
      zod.string(), zod.boolean())
      .refine(data => Object.keys(data).length >= 3 && Object.keys(data).length <= 4,
        'ERREUR: Le nombre de réponses doit être entre 3 et 4.')
      .refine(data => Object.values(data).some(Boolean),
        'Il faut choisir au moins une réponse correcte.'),
    )
  })

  constructor() {
    effect(() => {
      this.responses.set({
        ...Object.fromEntries(this.question()
          .answers.map(answer => [answer.index, false])),
      })
    })
  }
}
