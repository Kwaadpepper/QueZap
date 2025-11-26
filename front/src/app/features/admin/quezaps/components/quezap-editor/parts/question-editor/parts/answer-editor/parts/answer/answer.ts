import {
  ChangeDetectionStrategy, Component, computed,
  input,
  output,
} from '@angular/core'

import { AnswerWithResponse } from '@quezap/domain/models'
import { QuestionIcon, QuestionIconType } from '@quezap/features/quizz/components/question-view/parts'

import { PhraseInput } from '../phrase-input/phrase-input'

export type AnswerInput = AnswerWithResponse

@Component({
  selector: 'quizz-answer',
  templateUrl: './answer.html',
  imports: [QuestionIcon, PhraseInput],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerComponent {
  readonly answer = input.required<AnswerInput>()
  readonly readonly = input.required<boolean>()
  readonly answerChanged = output<AnswerInput>()

  protected readonly index = computed(() => this.answer().index)
  protected readonly phrase = computed(() => this.answer().value ?? '')
  protected readonly currentForm = computed(() => Object.values(QuestionIconType)[this.index()])

  protected onPhraseChanged(newPhrase: string) {
    this.answerChanged.emit({
      ...this.answer(),
      value: newPhrase,
    })
  }
}
