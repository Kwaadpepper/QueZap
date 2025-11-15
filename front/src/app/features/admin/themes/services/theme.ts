import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'

export interface NewThemeDTO {
  name: string
}

export interface ThemeService {
  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>>

  create(newTheme: NewThemeDTO): ServiceOutput<ThemeId>

  update(theme: Theme): ServiceOutput<void>
}

export const THEME_SERVICE = new InjectionToken<ThemeService>('ThemeService')
