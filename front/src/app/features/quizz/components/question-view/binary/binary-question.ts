import {
  ChangeDetectionStrategy, Component, computed, effect, inject, input, model, signal,
} from '@angular/core'
import { form, validateStandardSchema } from '@angular/forms/signals'

import * as zod from 'zod/v4'

import { BinaryQuestion, PictureUrl, QuestionId, QuestionWithAnswers } from '@quezap/domain/models'
import { TimerStore } from '@quezap/features/quizz/stores'

import { Picture, PrintableAnswer, QuestionAlert, QuestionAnswer } from '../parts'

type Responses = Record<number, boolean>

@Component({
  selector: 'quizz-question-binary',
  imports: [
    Picture,
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
  private readonly timerStore = inject(TimerStore)

  protected readonly timeLeft = computed<number | undefined>(() => this.timerStore.timeLeft())

  // Question related signals
  readonly question = input.required<BinaryQuestion & QuestionWithAnswers>()
  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)

  // Answers related signals
  protected readonly answers = computed<PrintableAnswer[]>(() => this.question().answers)
  protected readonly responses = model<Responses>({})

  protected readonly hasAnswered = computed<boolean>(() => this.responseForm().valid())
  protected readonly cannotAnswerAnymore = computed<boolean>(() =>
    this.timeLeft() === 0 && this.timeLeft() !== undefined)

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
