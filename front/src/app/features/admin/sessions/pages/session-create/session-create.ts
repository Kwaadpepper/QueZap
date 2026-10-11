import { Component, computed, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormsModule } from '@angular/forms'
import { form, submit, validateStandardSchema } from '@angular/forms/signals'
import { Router, RouterLink } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { InputText } from 'primeng/inputtext'
import { Select } from 'primeng/select'
import * as zod from 'zod/v4'

import { Pagination } from '@quezap/core/types'
import { QuezapWithTheme } from '@quezap/domain/models'

import { QUEZAP_SERVICE } from '../../../quezaps/services/quezap'
import { SESSION_SERVICE } from '../../services/session'

@Component({
  selector: 'quizz-session-create',
  standalone: true,
  imports: [Button, InputText, Select, FormsModule, RouterLink],
  templateUrl: './session-create.html',
})
export class SessionCreate {
  private sessionService = inject(SESSION_SERVICE)
  private quezapService = inject(QUEZAP_SERVICE)
  private router = inject(Router)
  private messageService = inject(MessageService)
  private destroyRef = inject(DestroyRef)

  quezaps = signal<QuezapWithTheme[]>([])

  quezapOptions = computed(() => {
    return this.quezaps().map(q => ({
      id: q.id,
      title: `${q.title} (${q.totalQuestions ?? 0} question${(q.totalQuestions ?? 0) > 1 ? 's' : ''})`,
      disabled: (q.totalQuestions ?? 0) === 0,
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

  model = signal({ name: '', quezapId: '' })

  sessionForm = form(this.model, (path) => {
    validateStandardSchema(path, zod.object({
      name: zod.string().min(1, 'Le nom est requis'),
      quezapId: zod.string().min(1, 'Le Quezap est requis'),
    }))
  })

  updateName(name: string) {
    this.model.update(m => ({ ...m, name }))
  }

  updateQuezapId(quezapId: string) {
    this.model.update(m => ({ ...m, quezapId }))
  }

  onSave() {
    if (!this.sessionForm().valid()) {
      this.sessionForm().markAsTouched()
      return
    }
    submit(this.sessionForm, async (f) => {
      this.sessionService
        .createSession(f.quezapId().value(), f.name().value())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((res) => {
          if (res.kind === 'success') {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Session créée' })
            this.router.navigate(['/admin/sessions'])
          }
          else {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: res.error.message || 'Création échouée',
            })
          }
        })
    })
  }
}
