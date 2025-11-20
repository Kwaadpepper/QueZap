import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { Tag } from 'primeng/tag'

import { Theme } from '@quezap/domain/models'

@Component({
  selector: 'quizz-question-theme',
  imports: [Tag],
  templateUrl: './question-theme.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTheme {
  readonly theme = input.required<Theme>()
}
