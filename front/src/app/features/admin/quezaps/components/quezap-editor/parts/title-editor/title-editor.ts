import {
  ChangeDetectionStrategy, Component, computed,
  inject,
  signal,
} from '@angular/core'
import { FormsModule } from '@angular/forms'

import { AutoFocusModule } from 'primeng/autofocus'
import { Inplace, InplaceModule } from 'primeng/inplace'
import { InputTextModule } from 'primeng/inputtext'

import { Quezap } from '@quezap/domain/models'

import { QuezapEditorContainer } from '../../editor-container'

export type TitleInput = Pick<Quezap, 'title'>

@Component({
  selector: 'quizz-title-editor',
  imports: [
    Inplace,
    InplaceModule,
    InputTextModule,
    AutoFocusModule,
    FormsModule,
  ],
  templateUrl: './title-editor.html',
  styleUrl: './title-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TitleEditor {
  private readonly editorContainer = inject(QuezapEditorContainer)

  private readonly quezap = computed<TitleInput>(() => this.editorContainer.quezap())
  protected readonly editedTitle = signal<string>('')

  protected openCallback() {
    this.editedTitle.set(this.quezap().title)
  }

  // * closeCallback est appelé par PrimeNG lors de la désactivation
  protected closeCallback() {
    const updatedQuezap = {
      ...this.editorContainer.quezap(),
      title: this.editedTitle().trim(),
    }
    this.editorContainer.setQuezap(updatedQuezap)
  }
}
