import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core'

import { QuestionWithAnswers, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

import { QuestionEditor, QuestionListView } from './parts'

export type QuezapEditorInput = Omit<QuezapWithQuestionsAndAnswers, 'id'>

@Component({
  selector: 'quizz-quezap-editor',
  imports: [QuestionListView, QuestionEditor],
  templateUrl: './quezap-editor.html',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapEditor {
  readonly quezap = input.required<QuezapEditorInput>()

  protected readonly questions = signal<QuestionWithAnswers[]>([])
}
