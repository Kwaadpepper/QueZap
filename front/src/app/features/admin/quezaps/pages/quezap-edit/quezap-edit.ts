import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'
import { QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'

import { QuezapEditor } from '../../components/quezap-editor/quezap-editor'

@Component({
  selector: 'quizz-quezap-edit',
  imports: [QuezapEditor],
  templateUrl: './quezap-edit.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapEdit {
  readonly #closeUrl = '/admin/quezaps'
  private readonly router = inject(Router)
  private readonly layout = inject(LayoutSettings)

  readonly quezap = input<QuezapWithQuestionsAndAnswers>(
    this.router.currentNavigation()?.extras.state?.['quezap'],
  )

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

  protected onCloseEditor() {
    this.router.navigateByUrl(this.#closeUrl)
  }
}
