import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { BooleanQuestion } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-boolean',
  imports: [],
  templateUrl: './boolean-question.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BooleanQuestionView {
  readonly question = input<BooleanQuestion>()
}
