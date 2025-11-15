import { Component, computed, ErrorHandler, inject, input, model, output, signal } from '@angular/core'
import { Field, form, submit, validateStandardSchema } from '@angular/forms/signals'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Dialog } from 'primeng/dialog'
import { InputText } from 'primeng/inputtext'
import { Message } from 'primeng/message'
import { catchError, concatMap, firstValueFrom, of, throwError } from 'rxjs'

import { HandledFrontError, ValidationError } from '@quezap/core/errors'
import { zod } from '@quezap/core/tools'
import { isFailure } from '@quezap/core/types'
import { Theme, ThemeId } from '@quezap/domain/models'
import { FieldError } from '@quezap/shared/directives'

import { NewThemeDTO, THEME_SERVICE } from '../../services'

export interface ThemeInputModel {
  id?: ThemeId
  name: string
}

@Component({
  selector: 'quizz-theme-editor',
  imports: [
    Dialog,
    InputText,
    FieldError,
    Field,
    Button,
    Message,
  ],
  templateUrl: './theme-editor.html',
})
export class ThemeEditor {
  private readonly errorHandler = inject(ErrorHandler)
  private readonly themeService = inject(THEME_SERVICE)
  private readonly message = inject(MessageService)

  public readonly theme = input.required<ThemeInputModel>()
  public readonly themePersisted = output<Theme>()
  public readonly visible = model.required<boolean>()

  protected readonly errorOccurred = signal(false)
  protected readonly forUpdateAction = computed(() => this.theme().id !== undefined)

  protected readonly themeData = signal({
    name: '',
  })

  protected readonly themeForm = form(this.themeData, (path) => {
    validateStandardSchema(path, zod.object({
      name: zod.string()
        .nonempty('Le nom ne peut pas être vide'),
    }))
  })

  protected onShow() {
    this.themeData.update(() => ({
      name: this.theme().name,
    }))
    this.themeForm().reset()
    this.errorOccurred.set(false)
  }

  protected onSave() {
    if (this.themeForm().invalid()) {
      this.errorHandler.handleError(new Error('Formulaire invalide'))
      return
    }

    this.errorOccurred.set(false)

    if (this.theme().id === undefined) {
      this.createTheme({
        name: this.themeForm.name().value(),
      })
      return
    }

    this.updateTheme({
      id: this.theme().id!,
      name: this.themeForm.name().value(),
    })
  }

  protected onCancel() {
    this.visible.set(false)
  }

  private createTheme(theme: NewThemeDTO) {
    submit(this.themeForm, () =>
      firstValueFrom(this.themeService.create(theme).pipe(
        concatMap((result) => {
          if (isFailure(result)) {
            const err = result.error
            if (err instanceof ValidationError) {
              return of(err.getErrorsForForm(this.themeForm))
            }

            // Unexpected error
            return throwError(() => err)
          }

          this.message.add({
            severity: 'success',
            summary: 'Thème crée',
            detail: 'Le thème a bien été créé',
            life: 5000,
          })
          this.visible.set(false)
          this.themePersisted.emit({
            ...theme,
            id: result.result,
          })

          return of(void 0)
        }),
        catchError((err) => {
          this.errorOccurred.set(true)

          this.errorHandler.handleError(
            HandledFrontError.from(err),
          )

          return of(void 0)
        }),
      ),
      ),
    )
  }

  private updateTheme(theme: Theme) {
    submit(this.themeForm, () =>
      firstValueFrom(this.themeService.update(theme).pipe(
        concatMap((result) => {
          if (isFailure(result)) {
            const err = result.error
            if (err instanceof ValidationError) {
              return of(err.getErrorsForForm(this.themeForm))
            }

            // Unexpected error
            return throwError(() => err)
          }

          this.message.add({
            severity: 'success',
            summary: 'Thème mis à jour',
            detail: 'Le thème a bien été mis à jour',
            life: 5000,
          })
          this.visible.set(false)
          this.themePersisted.emit(theme)

          return of(void 0)
        }),
        catchError((err) => {
          this.errorOccurred.set(true)

          this.errorHandler.handleError(
            HandledFrontError.from(err),
          )

          return of(void 0)
        }),
      ),
      ),
    )
  }
}
