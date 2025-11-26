import {
  ChangeDetectionStrategy, Component, computed, effect, inject,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { AutoFocusModule } from 'primeng/autofocus'
import { Inplace, InplaceModule } from 'primeng/inplace'
import { InputTextModule } from 'primeng/inputtext'

import { Question } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../../../editor-container'

export type PhraseInput = Pick<Question, 'value'>

@Component({
  selector: 'quizz-phrase-editor',
  imports: [
    Inplace,
    InplaceModule,
    InputTextModule,
    AutoFocusModule,
    FormsModule,
  ],
  templateUrl: './phrase-editor.html',
  styleUrl: './phrase-editor.css',
  styles: ':host { display: block; width: 100%;  }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhraseEditor {
  private readonly editorContainer = inject(QuezapEditorContainer)

  private readonly question = computed<PhraseInput>(() =>
    this.editorContainer.selectedQuestion(),
  )

  protected readonly editedPhrase = signal<string>('')

  constructor() {
    effect(() => {
      this.editedPhrase.update(() => {
        const questionValue = this.question().value
        return questionValue
      })
    })
  }

  protected openCallback() {
    this.editedPhrase.set(this.question().value)
  }

  // * closeCallback est appelé par PrimeNG lors de la désactivation
  protected closeCallback() {
    const question = this.editorContainer.selectedQuestion()
    this.editorContainer.updateQuestionAtIdx(
      this.editorContainer.selectionQuestionIdx(),
      {
        ...question,
        value: this.editedPhrase().trim(),
      },
    )
  }
}
