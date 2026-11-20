import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { add, delete1, find, rename1, ThemeName } from '@quezap/core/api-client'
import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'

import { NewThemeDTO, ThemeService } from './theme'

@Injectable({ providedIn: 'root' })
export class ThemeHttpService implements ThemeService {
  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>> {
    const p = toPageBasedPagination(page)

    return from(
      find({
        query: {
          paginationDto: {
            page: p.page,
            perPage: p.pageSize,
          },
          queryDto: {},
        },
        querySerializer: {
          object: {
            style: 'form',
            explode: true,
          },
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
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new NotFoundError('Erreur lors du chargement des thèmes'),
        }),
      ),
    )
  }

  create(newTheme: NewThemeDTO): ServiceOutput<ThemeId> {
    return from(
      add({ body: { name: newTheme.name as unknown as ThemeName } }),
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
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ValidationError({ name: ['Erreur de création'] }, 'Erreur de création'),
        }),
      ),
    )
  }

  update(theme: Theme): ServiceOutput<void> {
    return from(
      rename1({
        path: { id: theme.id },
        body: { name: theme.name as unknown as ThemeName },
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
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ValidationError({ name: ['Erreur de modification'] }, 'Erreur de modification'),
        }),
      ),
    )
  }

  delete(themeId: ThemeId): ServiceOutput<void> {
    return from(
      delete1({ path: { id: themeId } }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({ name: ['Erreur de suppression'] }, 'Erreur de suppression'),
          }
        }

        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
      catchError(() =>
        of({
          kind: 'failure' as const,
          error: new ValidationError({ name: ['Erreur de suppression'] }, 'Erreur de suppression'),
        }),
      ),
    )
  }
}
