import {
  ChangeDetectionStrategy, Component, computed, effect, input, model, signal,
} from '@angular/core'
import { form, validateStandardSchema } from '@angular/forms/signals'

import { zod } from '@quezap/core/tools'
import {
  BinaryQuestion, PictureUrl, QuestionId, QuestionWithAnswers, Theme, Timer,
} from '@quezap/domain/models'

import {
  Picture, PrintableAnswer, QuestionAlert, QuestionAnswer, QuestionTheme, QuestionTimer,
} from '../parts'

type Responses = Record<number, boolean>

@Component({
  selector: 'quizz-question-binary',
  imports: [
    QuestionTimer,
    Picture,
    QuestionTheme,
    QuestionAnswer,
    QuestionAlert,
  ],
  templateUrl: './binary-question.html',
  styles: [`
    :host {
      width: 100%;
      height: 100%;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BinaryQuestionView {
  // Question related signals
  readonly question = input.required<BinaryQuestion & QuestionWithAnswers>()
  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)
  protected readonly theme = computed<Theme>(() => this.question().theme)

  // Timer related signals
  protected readonly started = signal<boolean>(false)
  protected readonly timer = computed<Timer | undefined>(() => this.question().limit)
  protected readonly timeLeft = signal<number>(0)

  // Answers related signals
  protected readonly answers = computed<PrintableAnswer[]>(() => this.question().answers)
  protected readonly responses = model<Responses>({})

  protected readonly hasAnswered = computed<boolean>(() => this.responseForm().valid())
  protected readonly cannotAnswerAnymore = computed<boolean>(() => this.timeLeft() <= 0 && this.timer() !== undefined)

  protected readonly responseForm = form(this.responses, (path) => {
    validateStandardSchema(path, zod.record(
      zod.string(), zod.boolean())
      .refine(data => Object.keys(data).length === 2,
        'ERREUR: Le nombre de réponses doit être de 2.')
      .refine(data => Object.values(data).filter(Boolean).length === 1,
        'Il faut choisir une seul réponse correcte.'),
    )
  })

  private readonly lastSelectedKey = signal<'0' | '1' | null>(null)

  constructor() {
    effect(() => {
      // * Initialize responses when question changes
      this.responses.set({
        ...Object.fromEntries(this.question()
          .answers.map(answer => [answer.index, false])),
      })
    })
    effect(() => {
      // * Start the timer when the question is set
      if (this.question()) {
        this.started.set(true)
      }
    })
    effect(() => {
      // * Handle RADIO behavior
      const currentResponses = this.responseForm().value()

      if (currentResponses[0] === true && this.lastSelectedKey() !== '0') {
        this.lastSelectedKey.set('0')
      }
      else if (currentResponses[1] === true && this.lastSelectedKey() !== '1') {
        this.lastSelectedKey.set('1')
      }

      const selectedKeys = Object.entries(currentResponses)
        .filter(([_, value]) => value === true)
        .map(([key]) => key)
      if (selectedKeys.length > 1) {
        this.responses.set({
          0: this.lastSelectedKey() === '0',
          1: this.lastSelectedKey() === '1',
        })
      }
    })
  }

  protected onTimeExhausted() {
    console.log('Time exhausted for question:', this.question().id)
  }
}
