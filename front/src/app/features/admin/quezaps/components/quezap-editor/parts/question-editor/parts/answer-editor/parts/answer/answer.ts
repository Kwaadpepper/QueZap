import {
  ChangeDetectionStrategy, Component, computed,
  input,
  output,
} from '@angular/core'

import { AnswerWithResponse } from '@quezap/domain/models'
import { QuestionIcon, QuestionIconType } from '@quezap/features/quizz/components/question-view/parts'

import { AnswerCorrectnessComponent } from '../answer-correctness/answer-correctness'
import { PhraseInput } from '../phrase-input/phrase-input'

export type AnswerInput = AnswerWithResponse

@Component({
  selector: 'quizz-answer',
  templateUrl: './answer.html',
  imports: [
    QuestionIcon,
    PhraseInput,
    AnswerCorrectnessComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerComponent {
  readonly answer = input.required<AnswerInput>()
  readonly readonly = input.required<boolean>()
  readonly answerChanged = output<AnswerInput>()

  protected readonly index = computed(() => this.answer().index)
  protected readonly phrase = computed(() => this.answer().value ?? '')
  protected readonly currentForm = computed(() => Object.values(QuestionIconType)[this.index()])
  protected readonly isCorrect = computed(() => this.answer().isCorrect)

  protected onPhraseChanged(newPhrase: string) {
    this.answerChanged.emit({
      ...this.answer(),
      value: newPhrase,
    })
  }

  protected onCorrectnessChanged(isCorrect: boolean) {
    this.answerChanged.emit({
      ...this.answer(),
      isCorrect,
    })
  }
}
