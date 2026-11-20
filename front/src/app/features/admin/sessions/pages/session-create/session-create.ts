import { Component, computed, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormsModule } from '@angular/forms'
import { form, FormField, submit, validateStandardSchema } from '@angular/forms/signals'
import { Router, RouterLink } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Select } from 'primeng/select'
import { firstValueFrom } from 'rxjs'
import * as zod from 'zod/v4'

import { Pagination } from '@quezap/core/types'
import { QuezapWithTheme } from '@quezap/domain/models'
import { FieldError } from '@quezap/shared/directives/field-error'

import { QUEZAP_SERVICE } from '../../../quezaps/services/quezap'
import { SESSION_SERVICE } from '../../services/session'
import { SessionPageStore } from '../../stores'

@Component({
  selector: 'quizz-session-create',
  standalone: true,
  imports: [Button, InputText, Select, FormsModule, FormField, FieldError, RouterLink],
  templateUrl: './session-create.html',
})
export class SessionCreate {
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly quezapService = inject(QUEZAP_SERVICE)
  private readonly sessionPage = inject(SessionPageStore, { optional: true })
  private readonly router = inject(Router)
  private readonly messageService = inject(MessageService)
  private readonly destroyRef = inject(DestroyRef)

  readonly quezaps = signal<QuezapWithTheme[]>([])

  readonly quezapOptions = computed(() => {
    return this.quezaps().map(q => ({
      id: q.id,
      title: `${q.title} (${q.totalQuestions ?? 0} question${(q.totalQuestions ?? 0) > 1 ? 's' : ''})`,
      disabled: (q.totalQuestions ?? 0) === 0,
    }))
  })

  readonly model = signal({ name: '', quezapId: '' })

  readonly sessionForm = form(this.model, (path) => {
    validateStandardSchema(path, zod.object({
      name: zod.string().min(1, 'Le nom est requis'),
      quezapId: zod.string().min(1, 'Le Quezap est requis'),
    }))
  })

  constructor() {
    this.quezapService
      .getQuezapPage({ page: 1, pageSize: 100 } as Pagination)
      .pipe(takeUntilDestroyed())
      .subscribe((res) => {
        if (res.kind === 'success') {
          this.quezaps.set(res.result.data)
        }
      })
  }

  updateQuezapId(quezapId: string) {
    this.model.update(m => ({ ...m, quezapId }))
    this.sessionForm.quezapId().markAsTouched()
    this.sessionForm.quezapId().markAsDirty()
  }

  onSave(presentAfterCreate = false): Promise<boolean> {
    if (!this.sessionForm().valid()) {
      this.sessionForm().markAsTouched()
      this.sessionForm.name().markAsDirty()
      this.sessionForm.quezapId().markAsDirty()
      return Promise.resolve(false)
    }

    return submit(this.sessionForm, async (f) => {
      try {
        const res = await firstValueFrom(
          this.sessionService
            .createSession(f.quezapId().value(), f.name().value())
            .pipe(takeUntilDestroyed(this.destroyRef)),
          { defaultValue: undefined },
        )

        if (!res) {
          return
        }

        if (res.kind === 'success') {
          this.sessionPage?.reload()
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: `Session créée (Code PIN : ${res.result.code ?? ''})`,
          })

          if (presentAfterCreate && res.result.id) {
            await this.router.navigate(['/admin/sessions', res.result.id, 'present'])
          }
          else {
            await this.router.navigate(['/admin/sessions'])
          }
        }
        else {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: res.error.message || 'Création échouée',
          })
        }
      }
      catch {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Création échouée',
        })
      }
    })
  }
}
