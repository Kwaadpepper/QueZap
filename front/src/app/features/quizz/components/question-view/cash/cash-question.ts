import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { CashQuestion, PictureUrl, QuestionId, QuestionWithAnswers } from '@quezap/domain/models'
import { SESSION_API_SERVICE } from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

import { Picture } from '../parts/picture/picture'
import { QuestionAlert } from '../parts/question-alert/question-alert'

@Component({
  selector: 'quizz-question-cash',
  imports: [
    Picture,
    QuestionAlert,
    FormsModule,
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
  private readonly sessionApi = inject(SESSION_API_SERVICE)

  readonly question = input.required<CashQuestion & QuestionWithAnswers>()

  protected readonly questionId = computed<QuestionId>(() => this.question().id)
  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)

  protected readonly isRevealing = computed<boolean>(() => this.sessionStore.lastReveal() !== undefined)
  protected readonly timeLeft = computed<number | undefined>(() => this.timerStore.timeLeft())
  protected readonly cannotAnswerAnymore = computed<boolean>(
    () => this.timeLeft() === 0 && this.timeLeft() !== undefined,
  )

  protected readonly textInput = signal<string>('')
  protected readonly hasSubmitted = signal<boolean>(false)
  protected readonly lastSubmittedText = signal<string>('')

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
        this.hasSubmitted.set(true)
      }
      else {
        this.textInput.set('')
        this.lastSubmittedText.set('')
        this.hasSubmitted.set(false)
      }
    })
  }

  protected submitAnswer(): void {
    if (this.cannotAnswerAnymore()) {
      return
    }
    const trimmed = this.textInput().trim()
    if (!trimmed) {
      return
    }
    this.lastSubmittedText.set(trimmed)
    this.hasSubmitted.set(true)
    this.sessionStore.submitMyChoice(0, trimmed, this.question().id)
    this.sessionApi.submitAnswer(0, trimmed).subscribe()
  }
}
