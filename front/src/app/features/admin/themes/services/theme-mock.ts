import { HttpErrorResponse } from '@angular/common/http'
import { Injectable, signal } from '@angular/core'

import { delay, map, of, tap } from 'rxjs'
import * as zod from 'zod/v4'

import { NotFoundError, ServiceError, ValidationError } from '@quezap/core/errors'
import { zodToExternalValidationError } from '@quezap/core/tools/zod-to-external-validation-error'
import { PageOf, Pagination, ServiceOutput, toPageBasedPagination } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'
import { UUID } from '@quezap/domain/types'

import { NewThemeDTO, ThemeService } from './theme'
import { MOCK_THEMES } from './theme.mock'

const newThemeValidationschema = zod.object({
  name: zod.string()
    .min(2, 'Le nom doit contenir au moins 2 caractères')
    .max(50, 'Le nom ne peut pas dépasser 50 caractères')
    .nonempty('Le nom ne peut pas être vide'),
})

const themeValidationschema = zod.object({
  id: zod.uuid('UUID invalide'),
  name: newThemeValidationschema.shape.name,
})

@Injectable()
export class ThemeMockService implements ThemeService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)
  private readonly MOCK_ERROR = () => Math.random() < 0.2

  private readonly mockedThemes = signal<Theme[]>(MOCK_THEMES)

  getThemePage(page: Pagination): ServiceOutput<PageOf<Theme>> {
    const pagination = toPageBasedPagination(page)
    const themes: Theme[] = this.mockedThemes().map(product => ({
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
          console.debug('Mock: error while fetching themes')
          throw new ServiceError('Mock service error: themes')
        }
      }),
      map((obs) => {
        console.debug('Mock: fetched themes page', pageOfThemes)
        return {
          kind: 'success',
          result: obs,
        }
      }),
    )
  }

  create(newTheme: NewThemeDTO): ServiceOutput<ThemeId> {
    return of(newTheme).pipe(
      delay(this.MOCK_DELAY()),
      map((newTheme) => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while creating theme')
          throw new HttpErrorResponse({})
        }

        const parsed = newThemeValidationschema.safeParse(newTheme)

        if (parsed.success === false) {
          console.debug('Mock: invalid new theme data')
          return zodToExternalValidationError(parsed.error)
        }

        if (this.themeExists(newTheme.name)) {
          console.debug('Mock: theme name already exists')
          return new ValidationError({ name: ['Un thème avec ce nom existe déjà'] }, 'Un thème avec ce nom existe déjà')
        }

        const newId = crypto.randomUUID() as ThemeId
        this.mockedThemes.update(themes => [
          {
            id: newId,
            name: newTheme.name,
          },
          ...themes,
        ])

        console.debug('Mock: created new theme with id', newId)
        return {
          kind: 'success',
          result: newId,
        }
      }),
    )
  }

  update(theme: Theme): ServiceOutput<void> {
    return of(theme).pipe(
      delay(this.MOCK_DELAY()),
      map((theme) => {
        if (this.MOCK_ERROR()) {
          throw new HttpErrorResponse({})
        }

        const parsed = themeValidationschema.safeParse(theme)

        if (parsed.success === false) {
          console.debug('Mock: invalid theme data')
          return zodToExternalValidationError(parsed.error)
        }

        if (this.themeExists(theme.name, theme.id)) {
          console.debug('Mock: theme name already exists')
          return new ValidationError({ name: ['Un thème avec ce nom existe déjà'] }, 'Un thème avec ce nom existe déjà')
        }

        const existingTheme = this.getTheme(theme.id)

        if (!existingTheme) {
          console.debug('Mock: theme not found')
          return new NotFoundError('Thème non trouvé')
        }

        this.mockedThemes.update(themes => themes.map((t) => {
          if (t.id === theme.id) {
            return {
              ...t,
              name: theme.name,
            }
          }
          return t
        }))

        return {
          kind: 'success',
          result: void 0,
        }
      }),
    )
  }

  private getTheme(id: UUID): Theme | undefined {
    return this.mockedThemes().find(theme => theme.id === id)
  }

  private themeExists(name: string, ignore?: UUID): boolean {
    return this.mockedThemes().some(theme => theme.name.toLowerCase() === name.toLowerCase() && theme.id !== ignore)
  }
}
