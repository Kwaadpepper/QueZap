import { ChangeDetectionStrategy, Component, effect, model } from '@angular/core'

import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'

import { TypeSelector } from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [TypeSelector],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
  readonly question = model.required<QuestionEditorInput>()

  protected readonly QuestionTypeFrom = QuestionTypeFrom

  constructor() {
    effect(() => {
      console.log('Question changed:', this.question())
    })
  }
}
