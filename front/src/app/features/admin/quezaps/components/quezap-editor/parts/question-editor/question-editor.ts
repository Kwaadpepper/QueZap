import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core'

import { Divider } from 'primeng/divider'

import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../editor-container'

import { LimitSelector, PhraseEditor, QuestionTimer, TypeSelector } from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [
    TypeSelector,
    LimitSelector,
    Divider,
    PhraseEditor,
    QuestionTimer,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
  private readonly editorContainer = inject(QuezapEditorContainer)

  protected readonly question = computed<QuestionEditorInput>(() =>
    this.editorContainer.selectedQuestion(),
  )

  protected readonly QuestionTypeFrom = QuestionTypeFrom

  constructor() {
    effect(() => {
      console.log('Question changed:', this.question())
    })
  }
}
