import { ChangeDetectionStrategy, Component, effect, model } from '@angular/core'

import { Divider } from 'primeng/divider'

import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes'

import { LimitSelector, TypeSelector } from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [TypeSelector, LimitSelector, Divider, MinutesPipe],
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
