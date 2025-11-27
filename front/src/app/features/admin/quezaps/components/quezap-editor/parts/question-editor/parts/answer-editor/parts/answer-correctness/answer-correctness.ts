import { Component, effect, input, model, output } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { CheckboxModule } from 'primeng/checkbox'

@Component({
  selector: 'quizz-answer-correctness',
  template: `
    <p-checkbox
      type="checkbox"
      size="large"
      [pt]="{ box: { style: 'border-radius: 3rem; border: 2px solid #fff;' } }"
      [binary]="true"
      [value]="true"
      [trueValue]="true"
      [falseValue]="false"
      (onChange)="onCorrectnessChanged($event.checked)"
      [(ngModel)]="_checked"
    />
  `,
  imports: [
    CheckboxModule,
    FormsModule,
  ],
})
export class AnswerCorrectnessComponent {
  // * PrimeNg Checkbox requires ngModel binding
  protected readonly _checked = model<boolean>(false)

  readonly checked = input.required<boolean>()
  readonly correctnessChanged = output<boolean>()

  constructor() {
    effect(() => {
      this._checked.update(() => this.checked())
    }, { debugName: 'AnswerCorrectnessComponent:checked' })
  }

  protected onCorrectnessChanged(isCorrect: boolean) {
    this.correctnessChanged.emit(isCorrect)
  }
}
