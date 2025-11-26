import {
  ChangeDetectionStrategy, Component, computed,
  input,
} from '@angular/core'
import { Field, FieldTree } from '@angular/forms/signals'

import { CheckboxModule } from 'primeng/checkbox'

import { PictureUrl, QuestionId } from '@quezap/domain/models'

import { Picture } from '../picture/picture'
import { QuestionIcon, QuestionIconType } from '../question-icon/question-icon'

export interface PrintableAnswer {
  readonly index: number
  readonly value?: string
  readonly picture?: PictureUrl
}

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
  readonly answer = input.required<PrintableAnswer>()
  readonly field = input.required<FieldTree<boolean>>()
  readonly radio = input<boolean>(false)

  protected readonly identifier = computed<number>(() => this.answer()?.index ?? 0)
  protected readonly phrase = computed<string>(() => this.answer()?.value ?? '')
  protected readonly pictureUrl = computed<PictureUrl | undefined>(() => this.answer()?.picture ?? undefined)

  // --- Compute icon form based on question ID and answer index ---
  private readonly forms = Object.values(QuestionIconType)

  protected readonly uniqueSeed = computed<number>(() => {
    const questionId = String(this.questionId())
    const questionIdPart = Number(
      questionId.split('-').map(v => `0x${v}`).map(Number)
        .map(n => n % 100)
        .reduce((a, b) => a + b, ''),
    )

    return questionIdPart % this.forms.length
  })

  protected readonly currentFormIndex = computed<number>(() => (
    this.uniqueSeed() + this.identifier()
  ) % this.forms.length)

  protected readonly currentForm = computed<QuestionIconType>(() => this.forms[this.currentFormIndex()])
}
