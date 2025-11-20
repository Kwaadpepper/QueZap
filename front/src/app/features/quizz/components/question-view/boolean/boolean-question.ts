import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { BooleanQuestion } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-boolean',
  imports: [],
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
  readonly question = input.required<BooleanQuestion>()
}
