import {
  ChangeDetectionStrategy, Component, computed,
  effect,
  inject,
  input, model, signal,
} from '@angular/core'
import { form, validateStandardSchema } from '@angular/forms/signals'

import { zod } from '@quezap/core/tools'
import { BooleanQuestion, PictureUrl, QuestionId } from '@quezap/domain/models'
import { TimerStore } from '@quezap/features/quizz/stores'

import { Picture, PrintableAnswer, QuestionAlert, QuestionAnswer, QuestionTheme } from '../parts'

type Responses = Record<number, boolean>

@Component({
  selector: 'quizz-question-boolean',
  imports: [
    Picture,
    QuestionTheme,
    QuestionAnswer,
    QuestionAlert,
  ],
  templateUrl: './boolean-question.html',
  styles: [`
    :host {
      width: 100%;
      height: 100%;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BooleanQuestionView {
  private readonly timerStore = inject(TimerStore)

  protected readonly timeLeft = computed<number | undefined>(() => this.timerStore.timeLeft())

  // Question related signals
  readonly question = input.required<BooleanQuestion>()
  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)

  // Answers related signals
  readonly #defaultAnswers: PrintableAnswer[] = [
    { index: 0, value: 'Vrai' },
    { index: 1, value: 'Faux' },
  ]

  protected readonly answers = signal<PrintableAnswer[]>([...this.#defaultAnswers])

  protected readonly responses = model<Responses>({
    0: false,
    1: false,
  })

  protected readonly hasAnswered = computed<boolean>(() => this.responseForm().valid())
  protected readonly cannotAnswerAnymore = computed<boolean>(() => this.timeLeft() === 0
    && this.timeLeft() !== undefined)

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
        0: false,
        1: false,
      })
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
}
