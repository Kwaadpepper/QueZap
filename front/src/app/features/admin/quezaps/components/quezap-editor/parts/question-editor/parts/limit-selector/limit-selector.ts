import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { Select, SelectChangeEvent } from 'primeng/select'

import { Question } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes/minutes'

import { QuezapEditorStore } from '../../../../../../stores'

export type LimitSelectorInput = Pick<Question, 'limit'>

let uniqueId = 0

interface QuestionLimitOption {
  label: string
  value: number
}

@Component({
  selector: 'quizz-limit-selector',
  imports: [Select, FormsModule],
  templateUrl: './limit-selector.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LimitSelector {
  readonly #secondOptions = [5, 10, 15, 20, 25, 30, 40, 50, 60, 90, 120, 150, 180, 240, 300]
  readonly #defaultSeconds = 30
  private readonly editorStore = inject(QuezapEditorStore)

  readonly question = computed<LimitSelectorInput>(() =>
    this.editorStore.selectedQuestion(),
  )

  /** Limits in seconds */
  protected readonly availableLimitSeconds = signal<QuestionLimitOption[]>(
    this.#secondOptions.map(seconds => ({
      label: MinutesPipe.secondsToLabel(seconds),
      value: seconds,
    })),
  )

  protected selectedLimit = signal<number>(this.#defaultSeconds)

  protected readonly quickLimits = [10, 20, 30, 60, 90]

  protected readonly selectId = `quezap-question-limit-selector-${uniqueId++}`

  protected setQuickLimit(sec: number): void {
    this.selectedLimit.set(sec)
    this.onLimitChange({ value: sec } as SelectChangeEvent)
  }

  constructor() {
    effect(() => {
      const currentLimit = this.question().limit?.seconds
      const normalizedSeconds = currentLimit !== undefined && this.#secondOptions.includes(currentLimit)
        ? currentLimit
        : this.#defaultSeconds
      this.selectedLimit.set(normalizedSeconds)
    })
  }

  protected onLimitChange(event: SelectChangeEvent) {
    const newLimit = event.value as number
    const updatedQuestion = {
      ...this.editorStore.selectedQuestion(),
      limit: newLimit > 0
        ? { seconds: newLimit }
        : undefined,
    }
    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      updatedQuestion,
    )
  }
}
