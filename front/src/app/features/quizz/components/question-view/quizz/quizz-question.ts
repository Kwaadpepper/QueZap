import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { QuizzQuestion } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-quizz',
  imports: [],
  templateUrl: './quizz-question.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizzQuestionView {
  readonly question = input<QuizzQuestion>()
}
