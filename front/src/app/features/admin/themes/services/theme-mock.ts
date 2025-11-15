import { Injectable } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Theme } from '@quezap/domain/models'

import { ThemeService } from './theme'
import { MOCK_THEMES } from './theme.mock'

@Injectable()
export class ThemeMockService implements ThemeService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)
  private readonly MOCK_ERROR = () => Math.random() < 0.2

  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>> {
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
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map((obs) => {
        return {
          kind: 'success',
          result: obs,
        }
      }),
    )
  }

  update(theme: Theme): ServiceOutput<void> {
    return of(theme).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map(() => ({
        kind: 'success',
        result: void 0,
      })),
    )
  }
}
