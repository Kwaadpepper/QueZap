import { InjectionToken } from '@angular/core'

import { PageOf, Pagination, ServiceOutput } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'

export interface ThemeService {
  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>>
}

export const THEME_SERVICE = new InjectionToken<ThemeService>('ThemeService')
