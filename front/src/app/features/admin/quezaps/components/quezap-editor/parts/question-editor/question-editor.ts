import { ChangeDetectionStrategy, Component } from '@angular/core'

import { ButtonModule } from 'primeng/button'
import { Divider } from 'primeng/divider'

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
    Divider,
    PhraseEditor,
    QuestionTimer,
    ButtonModule,
    AnswerEditor,
    PanelQuestionActions,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
}
