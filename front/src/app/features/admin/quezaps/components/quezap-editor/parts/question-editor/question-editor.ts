import { ChangeDetectionStrategy, Component, effect, model } from '@angular/core'

import { Divider } from 'primeng/divider'

import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
import { MinutesPipe } from '@quezap/shared/pipes/minutes'

import { LimitSelector, PhraseEditor, QuestionTimer, TypeSelector } from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [
    TypeSelector,
    LimitSelector,
    Divider,
    MinutesPipe,
    PhraseEditor,
    QuestionTimer,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
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
