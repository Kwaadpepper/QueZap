import { Injectable } from '@angular/core'

import { from, map } from 'rxjs'

import { PageOfDtoThemeDto, ThemeIdDto } from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'

import { NewThemeDTO, ThemeService } from './theme'

@Injectable({ providedIn: 'root' })
export class ThemeHttpService implements ThemeService {
  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>> {
    const p = toPageBasedPagination(page)

    return from(
      client.get<{ 200: PageOfDtoThemeDto }>({
        url: '/apiv1/themes',
        query: {
          page: p.page,
          pageSize: p.pageSize,
        },
      }),
    ).pipe(
      map(({ data, error }) => {
        if (error || !data) {
          return {
            kind: 'failure' as const,
            error: new NotFoundError('Erreur lors du chargement des thèmes'),
          }
        }

        return {
          kind: 'success' as const,
          result: {
            data: (data.data ?? []).map(t => ({
              id: (t.id || t.uuid) as ThemeId,
              name: t.name ?? '',
            })),
            totalElements: data.totalElements ?? 0,
            totalPages: data.totalPages ?? 0,
            page: data.page ?? 0,
            pageSize: data.pageSize ?? p.pageSize,
            hasNext: data.hasNext ?? false,
            hasPrevious: data.hasPrevious ?? false,
          },
        }
      }),
    )
  }

  create(newTheme: NewThemeDTO): ServiceOutput<ThemeId> {
    return from(
      client.post<{ 200: ThemeIdDto, 201: ThemeIdDto }>({
        url: '/apiv1/themes',
        body: { name: newTheme.name },
      }),
    ).pipe(
      map(({ data, error }) => {
        const id = data?.id || data?.uuid

        if (error || !id) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({ name: ['Erreur de création'] }, 'Erreur de création'),
          }
        }

        return {
          kind: 'success' as const,
          result: id as ThemeId,
        }
      }),
    )
  }

  update(theme: Theme): ServiceOutput<void> {
    return from(
      client.patch<void>({
        url: `/apiv1/themes/${theme.id}`,
        body: { name: theme.name },
      }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({ name: ['Erreur de modification'] }, 'Erreur de modification'),
          }
        }

        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
    )
  }
}
