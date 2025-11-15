import { Component, ErrorHandler, inject, input, model, output, signal } from '@angular/core'
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
import { UUID } from '@quezap/domain/types'
import { FieldError } from '@quezap/shared/directives'

import { THEME_SERVICE } from '../../services'

export interface ThemeInputModel {
  id: UUID
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

  public readonly themeUpdated = output<ThemeInputModel>()

  protected readonly errorOccurred = signal(false)

  protected readonly themeForm = signal({
    name: '',
  })

  protected readonly editForm = form(this.themeForm, (path) => {
    validateStandardSchema(path, zod.object({
      name: zod.string()
        .nonempty('Le nom ne peut pas être vide'),
    }))
  })

  public readonly visible = model.required<boolean>()

  protected onShow() {
    this.themeForm.update(() => ({
      name: this.theme().name,
    }))
  }

  protected onSave() {
    if (this.editForm().invalid()) {
      this.errorHandler.handleError(new Error('Formulaire invalide'))
      return
    }

    const newTheme = {
      id: this.theme().id,
      name: this.editForm.name().value(),
    }

    this.errorOccurred.set(false)

    submit(this.editForm, () =>
      firstValueFrom(this.themeService.update(newTheme).pipe(
        concatMap((result) => {
          if (isFailure(result)) {
            const err = result.error
            if (err instanceof ValidationError) {
              return of(err.getErrorsForForm(this.editForm))
            }

            // Unexpected error
            return throwError(() => err)
          }

          this.message.add({
            severity: 'success',
            summary: 'Thème mis à jour',
            detail: 'Le thème a bien été mis à jour',
            life: 5000,
            sticky: true,
          })
          this.visible.set(false)
          this.themeUpdated.emit(newTheme)

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

  protected onCancel() {
    this.visible.set(false)
  }
}
