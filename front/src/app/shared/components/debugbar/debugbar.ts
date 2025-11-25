import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonIcon, ButtonModule } from 'primeng/button'

import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'

import { IconFacade } from '../icon/icon-facade'

@Component({
  selector: 'quizz-debugbar',
  imports: [
    ButtonModule,
    RouterModule,
    ButtonIcon,
    IconFacade,
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
