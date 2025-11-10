import { Injectable } from '@angular/core'

import { delay, map, Observable, of } from 'rxjs'

import { PageOf, Pagination, toPageBasedPagination } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'

import { ThemeService } from './theme'
import { MOCK_THEMES } from './theme.mock'

@Injectable()
export class ThemeMockService implements ThemeService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)
  private readonly NETWORK_ERROR_PROBABILITY = 0.5

  getThemePage(page: Pagination): Observable<PageOf<Theme>> {
    const pagination = toPageBasedPagination(page)
    const themes: Theme[] = MOCK_THEMES.map(product => ({
      id: product.id,
      name: product.name,
    }))

    const startIndex = (pagination.page - 1) * pagination.pageSize
    const endIndex = startIndex + pagination.pageSize
    const pagedThemes = themes.slice(startIndex, endIndex)

    const pageOfThemes: PageOf<Theme> = {
      data: pagedThemes,
      totalElements: themes.length,
      totalPages: Math.ceil(themes.length / pagination.pageSize),
      page: pagination.page,
      pageSize: pagination.pageSize,
      hasNext: endIndex < themes.length,
      hasPrevious: startIndex > 0,
    }

    return of(pageOfThemes).pipe(
      delay(this.MOCK_DELAY()),
      map((obs) => {
        if (Math.random() < this.NETWORK_ERROR_PROBABILITY) {
          throw new Error('Network error occurred while fetching themes.')
        }
        return obs
      }),
    )
  }
}
