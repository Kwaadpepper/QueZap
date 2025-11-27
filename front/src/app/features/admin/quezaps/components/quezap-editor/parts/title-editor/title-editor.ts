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

import { QuezapEditorStore } from '../../../../stores'

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
  private readonly editorStore = inject(QuezapEditorStore)

  private readonly quezap = computed<TitleInput>(() => this.editorStore.quezap())
  protected readonly editedTitle = signal<string>('')

  protected openCallback() {
    this.editedTitle.set(this.quezap().title)
  }

  // * closeCallback est appelé par PrimeNG lors de la désactivation
  protected closeCallback() {
    const updatedQuezap = {
      ...this.editorStore.quezap(),
      title: this.editedTitle().trim(),
    }
    this.editorStore.setQuezap(updatedQuezap)
  }
}
