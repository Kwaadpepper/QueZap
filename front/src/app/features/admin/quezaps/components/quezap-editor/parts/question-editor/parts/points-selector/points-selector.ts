import {
  ChangeDetectionStrategy, Component, computed, effect, inject, signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { Select, SelectChangeEvent } from 'primeng/select'

import { QuezapEditorStore } from '../../../../../../stores'

export interface PointOption {
  label: string
  quickLabel: string
  value: number
}

let uniqueId = 0

@Component({
  selector: 'quizz-points-selector',
  imports: [Select, FormsModule],
  templateUrl: './points-selector.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PointsSelector {
  private readonly editorStore = inject(QuezapEditorStore)

  protected readonly pointOptions: PointOption[] = [
    { label: 'Standard (1000 pts)', quickLabel: '1000 pts', value: 1000 },
    { label: 'Double (2000 pts)', quickLabel: 'Double (2000)', value: 2000 },
    { label: 'Sans points (0 pt)', quickLabel: '0 pt', value: 0 },
  ]

  protected readonly selectId = `quezap-question-points-selector-${uniqueId++}`

  readonly question = computed(() => this.editorStore.selectedQuestion())

  protected selectedPoints = signal<number>(1000)

  constructor() {
    effect(() => {
      const q = this.question()
      if (q && q.answers && q.answers.length > 0) {
        const currentPoints = q.answers[0]?.points ?? 1000
        this.selectedPoints.set(currentPoints)
      }
    })
  }

  protected setQuickPoints(points: number): void {
    this.selectedPoints.set(points)
    this.applyPointsChange(points)
  }

  protected onPointsChange(event: SelectChangeEvent): void {
    const points = event.value as number
    this.applyPointsChange(points)
  }

  private applyPointsChange(points: number): void {
    const currentQ = this.editorStore.selectedQuestion()
    if (!currentQ) return

    const updatedQuestion = {
      ...currentQ,
      answers: currentQ.answers.map(a => ({
        ...a,
        points,
      })),
    }

    this.editorStore.updateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
      updatedQuestion,
    )
  }
}
