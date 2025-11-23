import { ChangeDetectionStrategy, Component, effect, model, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { Select, SelectChangeEvent } from 'primeng/select'

import { Question } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes'

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
  readonly #secondOptions = [0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 90, 120, 150, 180, 240, 300]
  readonly question = model.required<LimitSelectorInput>()

  /** Limits in seconds */
  protected readonly availableLimitSeconds = signal<QuestionLimitOption[]>([
    { label: 'Pas de limite', value: 0 },
    this.#secondOptions.map(seconds => ({
      label: MinutesPipe.secondsToLabel(seconds),
      value: seconds,
    })),
  ].flat())

  protected selectedLimit = this.availableLimitSeconds()[0].value

  protected readonly selectId = `quezap-question-limit-selector-${uniqueId++}`

  constructor() {
    effect(() => {
      const currentLimit = this.question().limit
      const normalizedLimit = this.#secondOptions.includes(currentLimit?.seconds ?? 0)
        ? { seconds: currentLimit?.seconds ?? this.#secondOptions[0] }
        : { seconds: this.#secondOptions[0] }

      this.selectedLimit = normalizedLimit ? normalizedLimit.seconds : 0
    })
  }

  protected onLimitChange(event: SelectChangeEvent) {
    this.question.update((question) => {
      const newLimit = event.value as number
      return {
        ...question,
        limit: newLimit > 0
          ? { seconds: newLimit }
          : undefined,
      }
    })
  }
}
