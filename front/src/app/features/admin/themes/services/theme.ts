import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'
import { UUID } from '@quezap/domain/types'

export interface NewThemeDTO {
  name: string
}

export interface ThemeService {
  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>>

  create(newTheme: NewThemeDTO): ServiceOutput<UUID>

  update(theme: Theme): ServiceOutput<void>
}

export const THEME_SERVICE = new InjectionToken<ThemeService>('ThemeService')
