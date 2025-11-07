import { InjectionToken } from '@angular/core'

import { Observable } from 'rxjs'

import { PageOf, Pagination } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'

export interface ThemeService {
  getThemePage(page: Pagination): Observable<PageOf<Theme>>
}

export const THEME_SERVICE = new InjectionToken<ThemeService>('ThemeService')
