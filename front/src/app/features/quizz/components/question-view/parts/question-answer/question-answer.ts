import {
  ChangeDetectionStrategy, Component, computed,
  input,
} from '@angular/core'
import { FieldTree } from '@angular/forms/signals'

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

  // --- Unified forms and colors matching host projector (P1-3) ---
  private readonly forms = [
    QuestionIconType.Circle,
    QuestionIconType.Diamond,
    QuestionIconType.Cross,
    QuestionIconType.Hexagon,
  ]

  protected readonly isBooleanChoice = computed<boolean>(() => {
    const text = this.phrase().trim().toLowerCase()
    return text === 'vrai' || text === 'faux'
  })

  protected readonly currentFormIndex = computed<number>(() => {
    if (this.isBooleanChoice()) {
      return this.phrase().trim().toLowerCase() === 'vrai' ? 3 : 0
    }
    return this.identifier() % 4
  })

  protected readonly currentForm = computed<QuestionIconType>(() => {
    if (this.isBooleanChoice()) {
      return this.phrase().trim().toLowerCase() === 'vrai'
        ? QuestionIconType.Circle
        : QuestionIconType.Cross
    }
    return this.forms[this.identifier() % this.forms.length]
  })

  protected readonly formName = computed<string>(() => {
    switch (this.currentForm()) {
      case QuestionIconType.Circle: return 'Cercle'
      case QuestionIconType.Diamond: return 'Losange'
      case QuestionIconType.Cross: return 'Croix'
      case QuestionIconType.Hexagon: return 'Hexagone'
      case QuestionIconType.Triangle: return 'Triangle'
      case QuestionIconType.Square: return 'Carré'
      case QuestionIconType.Star: return 'Étoile'
      default: return 'Option'
    }
  })

  protected readonly accessibleLabel = computed<string>(() => {
    const num = this.identifier() + 1
    const shape = this.formName()
    const content = this.phrase().trim()
    return content ? `Option ${num} (${shape}) : ${content}` : `Option ${num} (${shape})`
  })
}
