import { ChangeDetectionStrategy, Component, effect, inject, model } from '@angular/core'

import { LayoutSettings } from '@quezap/core/services'

import { QuezapEditor, QuezapEditorInput } from '../../components/quezap-editor/quezap-editor'

@Component({
  selector: 'quizz-quezap-create',
  imports: [QuezapEditor],
  templateUrl: './quezap-create.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapCreate {
  private readonly layout = inject(LayoutSettings)

  protected readonly quezapModel = model<QuezapEditorInput>({
    title: '',
    description: '',
    questionWithAnswersAndResponses: [],
  })

  constructor() {
    effect((onCleanUp) => {
      this.layout.asWebsite.set(false)
      this.layout.inContainer.set(false)
      onCleanUp(() => {
        this.layout.asWebsite.set(true)
        this.layout.inContainer.set(true)
      })
    })
  }
}
