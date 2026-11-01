import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core'
import { Router } from '@angular/router'

import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'

import { QuezapEditor } from '../../components/quezap-editor/quezap-editor'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-create',
  imports: [QuezapEditor],
  templateUrl: './quezap-create.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapCreate {
  readonly #closeUrl = '/admin/quezaps'
  private readonly router = inject(Router)
  private readonly layout = inject(LayoutSettings)
  private readonly quezapPage = inject(QuezapPageStore, { optional: true })

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
    this.quezapPage?.reload()
    this.router.navigateByUrl(this.#closeUrl)
  }
}
