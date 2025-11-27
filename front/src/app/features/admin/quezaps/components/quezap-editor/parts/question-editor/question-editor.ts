import { ChangeDetectionStrategy, Component } from '@angular/core'

import { DividerModule } from 'primeng/divider'

import { QuestionWithAnswers } from '@quezap/domain/models'

import {
  AnswerEditor, LimitSelector,
  PanelQuestionActions,
  PhraseEditor, QuestionTimer, TypeSelector,
} from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [
    TypeSelector,
    LimitSelector,
    DividerModule,
    PhraseEditor,
    QuestionTimer,
    AnswerEditor,
    PanelQuestionActions,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
}
