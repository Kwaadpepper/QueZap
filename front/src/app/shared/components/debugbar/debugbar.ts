import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonDirective } from 'primeng/button'

import { Config, LayoutSettings } from '@quezap/core/services'

@Component({
  selector: 'quizz-debugbar',
  imports: [
    ButtonDirective,
    RouterModule,
  ],
  templateUrl: './debugbar.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Debugbar {
  private readonly config = inject(Config)
  private readonly layoutSettings = inject(LayoutSettings)

  protected readonly asWebsite = computed(() => this.layoutSettings.asWebsite())
  protected readonly debug = computed(() => this.config.debug())

  toggleWebsiteMode(): void {
    this.layoutSettings.asWebsite.update(mode => !mode)
  }
}
