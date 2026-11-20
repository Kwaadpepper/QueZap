import { DatePipe } from '@angular/common'
import {
  ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, RouterLink } from '@angular/router'

import { MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Message } from 'primeng/message'
import { TableModule } from 'primeng/table'

import { SessionReportDto } from '@quezap/core/api-client'
import { Spinner } from '@quezap/shared/components/spinner/spinner'

import { SESSION_SERVICE } from '../../services/session'

@Component({
  selector: 'quizz-session-report',
  templateUrl: './session-report.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    Button,
    Message,
    TableModule,
    Spinner,
  ],
})
export class SessionReport implements OnInit {
  private readonly route = inject(ActivatedRoute)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly messageService = inject(MessageService, { optional: true })
  private readonly destroyRef = inject(DestroyRef)

  protected readonly sessionId = signal<string>('')
  protected readonly report = signal<SessionReportDto | null>(null)
  protected readonly loading = signal<boolean>(true)
  protected readonly error = signal<string | null>(null)

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || ''
    this.sessionId.set(id)
    if (!id) {
      this.error.set('Identifiant de session invalide')
      this.loading.set(false)
      return
    }

    this.fetchReport(id)
  }

  protected fetchReport(id: string): void {
    this.loading.set(true)
    this.error.set(null)

    this.sessionService.getSessionReport(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        this.loading.set(false)
        if (output.kind === 'success') {
          this.report.set(output.result)
        }
        else {
          this.error.set(output.error.message || 'Impossible de charger le bilan de la session')
        }
      })
  }

  protected downloadCsv(): void {
    this.sessionService.exportCsv(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (output) => {
          if (output.kind === 'success') {
            const url = window.URL.createObjectURL(output.result)
            const a = document.createElement('a')
            a.href = url
            a.download = `session-${this.report()?.sessionNumber || this.sessionId()}-resultats.csv`
            a.click()
            window.URL.revokeObjectURL(url)
          }
          else {
            const detail = output.error.message || 'Échec du téléchargement du fichier CSV'
            this.error.set(detail)
            this.messageService?.add({
              severity: 'error',
              summary: 'Erreur',
              detail,
            })
          }
        },
        error: () => {
          const detail = 'Échec du téléchargement du fichier CSV'
          this.error.set(detail)
          this.messageService?.add({
            severity: 'error',
            summary: 'Erreur',
            detail,
          })
        },
      })
  }

  protected getSuccessBadgeSeverity(rate: number): 'success' | 'warn' | 'danger' {
    if (rate >= 70) return 'success'
    if (rate >= 40) return 'warn'
    return 'danger'
  }
}
