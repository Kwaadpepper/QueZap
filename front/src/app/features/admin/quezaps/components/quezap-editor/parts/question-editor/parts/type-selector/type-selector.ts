import { ChangeDetectionStrategy, Component, effect, model, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { SelectChangeEvent, SelectModule } from 'primeng/select'

import { Question, QuestionType, QuestionTypeFrom } from '@quezap/domain/models'

export type TypeSelectorInput = Pick<Question, 'type'>

interface QuestionTypeOption {
  label: string
  value: QuestionType
}

let uniqueId = 0

@Component({
  selector: 'quizz-type-selector',
  imports: [SelectModule, FormsModule],
  templateUrl: './type-selector.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TypeSelector {
  readonly question = model.required<TypeSelectorInput>()

  protected selectedType = QuestionType.Boolean

  protected readonly selectId = `quezap-question-type-selector-${uniqueId++}`

  protected readonly questionTypes = signal<QuestionTypeOption[]>(
    Object.values(QuestionType).map(type => ({
      label: QuestionTypeFrom(type).toString(),
      value: type,
    })),
  )

  constructor() {
    effect(() => {
      const currentType = this.question().type
      this.selectedType = currentType
    })
  }

  protected onTypeChange(event: SelectChangeEvent) {
    const newType = event.value as QuestionType
    this.question.set({ ...this.question(), type: newType })
  }
}
