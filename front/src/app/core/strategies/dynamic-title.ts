import { inject, Injectable } from '@angular/core'
import { Title } from '@angular/platform-browser'
import { RouterStateSnapshot, TitleStrategy } from '@angular/router'

import { environment } from '@quezap/env/environment'

import { Config, Environment } from '../services/config/config'

@Injectable({ providedIn: 'root' })
export class DynamicTitleStrategy extends TitleStrategy {
  private readonly config = inject(Config)
  private readonly title = inject(Title)

  override updateTitle(routerState: RouterStateSnapshot): void {
    const appNameEnvPrefix = environment.env === Environment.PROD ? '' : `[${environment.env.toUpperCase()}]`
    const appName = this.config.appConfig.value().appName
    const title = this.buildTitle(routerState)

    this.title.setTitle(
      title
        ? `${appNameEnvPrefix} ${title} • ${appName}`.trim()
        : `${appNameEnvPrefix} ${appName}`.trim(),
    )
  }
}
