import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { BinaryQuestion } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-binary',
  imports: [],
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
  readonly question = input<BinaryQuestion>()
}
