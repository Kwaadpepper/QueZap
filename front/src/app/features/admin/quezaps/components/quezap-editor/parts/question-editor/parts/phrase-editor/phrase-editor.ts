import { ChangeDetectionStrategy, Component, computed, model, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { AutoFocusModule } from 'primeng/autofocus'
import { Inplace, InplaceModule } from 'primeng/inplace'
import { InputTextModule } from 'primeng/inputtext'

import { Question } from '@quezap/domain/models'

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
  readonly question = model.required<PhraseInput>()

  protected readonly editedPhrase = signal<string>('')

  protected readonly phrase = computed(() => this.question().value)

  protected openCallback() {
    this.editedPhrase.set(this.question().value)
  }

  // * closeCallback est appelé par PrimeNG lors de la désactivation
  protected closeCallback() {
    this.question.update(q => ({
      ...q,
      value: this.editedPhrase().trim(),
    }))
  }
}
