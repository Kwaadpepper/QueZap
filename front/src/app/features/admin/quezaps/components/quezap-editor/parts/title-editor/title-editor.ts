import { ChangeDetectionStrategy, Component, computed, model, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { AutoFocusModule } from 'primeng/autofocus'
import { Inplace, InplaceModule } from 'primeng/inplace'
import { InputTextModule } from 'primeng/inputtext'

import { Quezap } from '@quezap/domain/models'

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
  readonly quezap = model.required<TitleInput>()

  protected readonly editedTitle = signal<string>('')

  // Le computed reste pertinent pour le texte affiché
  protected readonly phrase = computed(() => this.quezap().title)

  protected openCallback() {
    this.editedTitle.set(this.quezap().title)
  }

  // * closeCallback est appelé par PrimeNG lors de la désactivation
  protected closeCallback() {
    this.quezap.update(q => ({
      ...q,
      title: this.editedTitle().trim(),
    }))
  }
}
