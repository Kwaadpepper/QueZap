import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { QuizzQuestion } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-quizz',
  imports: [],
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
  readonly question = input<QuizzQuestion>()
}
