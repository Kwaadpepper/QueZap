import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { SelectChangeEvent, SelectModule } from 'primeng/select'

import { getNewAnswerWithResponse, Question, QuestionType, QuestionTypeFrom } from '@quezap/domain/models'

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
    const currentQuestion = this.editorStore.selectedQuestion()
    let answers = [...currentQuestion.answers]

    if (newType === QuestionType.Quizz) {
      // Pad to 4 answers if fewer exist
      while (answers.length < 4) {
        answers.push(getNewAnswerWithResponse(answers.length))
      }
    }
    else if (newType === QuestionType.Binary || newType === QuestionType.Boolean) {
      while (answers.length < 2) {
        answers.push(getNewAnswerWithResponse(answers.length))
      }
      answers = answers.slice(0, 2)
      let foundCorrect = false
      answers = answers.map((a) => {
        const isCorrect = Boolean(a.isCorrect) && !foundCorrect
        if (isCorrect) {
          foundCorrect = true
        }
        return {
          ...a,
          isCorrect,
          ...(newType === QuestionType.Boolean ? { picture: undefined } : {}),
        }
      })
    }
    else if (newType === QuestionType.Cash) {
      const nonEmptyAnswers = answers.filter(a => Boolean(a.value?.trim()))
      if (nonEmptyAnswers.length === 0) {
        answers = [{ ...getNewAnswerWithResponse(0), isCorrect: true, value: '', picture: undefined }]
      }
      else {
        answers = nonEmptyAnswers.slice(0, 4).map((a, idx) => ({
          ...a,
          index: idx,
          isCorrect: true,
          picture: undefined,
        }))
      }
    }

    // Re-index answers cleanly
    answers = answers.map((a, idx) => ({ ...a, index: idx }))

    const updatedQuestion = {
      ...currentQuestion,
      type: newType,
      answers,
    }
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      updatedQuestion,
    )
  }
}
