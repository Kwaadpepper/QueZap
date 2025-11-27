import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { SelectChangeEvent, SelectModule } from 'primeng/select'

import { Question, QuestionType, QuestionTypeFrom } from '@quezap/domain/models'

import { QuezapEditorStore } from '../../../../../../stores'

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
  private readonly editorStore = inject(QuezapEditorStore)
  private readonly question = computed<TypeSelectorInput>(() => this.editorStore.selectedQuestion())

  protected selectedType = signal<QuestionType>(QuestionType.Quizz)

  protected readonly selectId = `quezap-question-type-selector-${uniqueId++}`

  protected readonly questionTypes = signal<QuestionTypeOption[]>(
    Object.values(QuestionType).map(type => ({
      label: QuestionTypeFrom(type).toString(),
      value: type,
    })),
  )

  constructor() {
    effect(() => {
      this.selectedType.update(() => this.question().type)
    })
  }

  protected onTypeChange(event: SelectChangeEvent) {
    const newType = event.value as QuestionType
    const updatedQuestion = {
      ...this.editorStore.selectedQuestion(),
      type: newType,
    }
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      updatedQuestion,
    )
  }
}
