import { Component, computed, inject } from '@angular/core'

import { ProgressBar } from 'primeng/progressbar'

import { LoadingStatus } from '@quezap/core/services'

@Component({
  selector: 'quizz-loading-bar',
  imports: [ProgressBar],
  templateUrl: './loading-bar.html',
})
export class LoadingBar {
  private readonly loadingStatus = inject(LoadingStatus)

  protected readonly progression = computed(() => {
    const progression = this.loadingStatus.progression()
    console.debug('LoadingBar progression:', progression)

    return progression === null ? 0 : progression
  })
}
