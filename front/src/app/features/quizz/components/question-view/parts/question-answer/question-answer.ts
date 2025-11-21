import {
  ChangeDetectionStrategy, Component, computed, input,
  signal,
} from '@angular/core'
import { Field, FieldTree } from '@angular/forms/signals'

import { CheckboxModule } from 'primeng/checkbox'

import { Answer, PictureUrl, QuestionId } from '@quezap/domain/models'

import { Picture } from '../picture/picture'
import { QuestionIcon } from '../question-icon/question-icon'

@Component({
  selector: 'quizz-question-answer',
  imports: [
    Field,
    Picture,
    CheckboxModule,
    QuestionIcon,
  ],
  templateUrl: './question-answer.html',
  styleUrl: './question-answer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionAnswer {
  readonly questionId = input.required<QuestionId>()
  readonly answer = input.required<Answer>()
  readonly field = input.required<FieldTree<boolean>>()

  protected readonly identifier = computed<number>(() => this.answer()?.index ?? 0)
  protected readonly phrase = computed<string>(() => this.answer()?.value ?? '')
  protected readonly pictureUrl = computed<PictureUrl | undefined>(() => this.answer()?.picture ?? undefined)

  private readonly forms = signal([
    'circle', 'cross', 'diamond', 'hexagon',
    'pentagon', 'square', 'star', 'triangle',
  ])

  protected readonly uniqueSeed = computed<number>(() => {
    const questionId = String(this.questionId())
    const questionIdPart = Number(
      questionId.split('-').map(v => `0x${v}`).map(Number)
        .map(n => n % 100)
        .reduce((a, b) => a + b, ''),
    )

    return questionIdPart % this.forms().length
  })

  protected readonly currentFormIndex = computed<number>(() => (
    this.uniqueSeed() + this.identifier()
  ) % this.forms().length)

  protected readonly currentForm = computed<string>(() => this.forms()[this.currentFormIndex()])
}
