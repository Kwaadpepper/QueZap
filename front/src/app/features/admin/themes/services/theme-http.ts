import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'

import { NewThemeDTO, ThemeService } from './theme'

interface ThemeApiResponse {
  id: string
  name: string
}

interface PageOfApiResponse<T> {
  data: T[]
  totalElements: number
  totalPages: number
  page: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}

@Injectable({ providedIn: 'root' })
export class ThemeHttpService implements ThemeService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>> {
    const p = toPageBasedPagination(page)
    const params = new HttpParams()
      .set('page', p.page.toString())
      .set('pageSize', p.pageSize.toString())

    return this.http.get<PageOfApiResponse<ThemeApiResponse>>(`${this.apiUrl}/themes`, { params }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: {
          data: res.data.map(t => ({ id: t.id as ThemeId, name: t.name })),
          totalElements: res.totalElements,
          totalPages: res.totalPages,
          page: res.page,
          pageSize: res.pageSize,
          hasNext: res.hasNext,
          hasPrevious: res.hasPrevious,
        },
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new NotFoundError(error.message || 'Erreur lors du chargement des thèmes'),
      })),
    )
  }

  create(newTheme: NewThemeDTO): ServiceOutput<ThemeId> {
    return this.http.post<{ id: string }>(`${this.apiUrl}/themes`, { name: newTheme.name }).pipe(
      map(res => ({
        kind: 'success' as const,
        result: res.id as ThemeId,
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({ name: [error.message || 'Erreur de création'] }, error.message),
      })),
    )
  }

  update(theme: Theme): ServiceOutput<void> {
    return this.http.patch<void>(`${this.apiUrl}/themes/${theme.id}`, { name: theme.name }).pipe(
      map(() => ({
        kind: 'success' as const,
        result: undefined,
      })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({ name: [error.message || 'Erreur de modification'] }, error.message),
      })),
    )
  }
}
