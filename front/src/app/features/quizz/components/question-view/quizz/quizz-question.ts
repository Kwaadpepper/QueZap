import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core'

import { PictureUrl, QuizzQuestion, Theme, Timer } from '@quezap/domain/models'

import { Picture, QuestionTheme, QuestionTimer } from '../parts'

@Component({
  selector: 'quizz-question-quizz',
  imports: [QuestionTimer, Picture, QuestionTheme],
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
  readonly question = input.required<QuizzQuestion>()

  protected readonly phrase = computed<string>(() => this.question().value)
  protected readonly picture = computed<PictureUrl | undefined>(() => this.question().picture)
  protected readonly theme = computed<Theme>(() => this.question().theme)

  protected readonly started = signal<boolean>(false)
  protected readonly timer = computed<Timer | undefined>(() => this.question().limit)

  constructor() {
    effect(() => {
      // Start the timer when the question is set
      if (this.question()) {
        this.started.set(true)
      }
    })
  }

  protected onTimeExhausted() {
    console.log('Time exhausted for question:', this.question().id)
  }
}
