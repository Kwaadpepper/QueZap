import { DatePipe } from '@angular/common'
import { HttpClient } from '@angular/common/http'
import {
  ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, RouterLink } from '@angular/router'

import { Button } from 'primeng/button'
import { Message } from 'primeng/message'
import { TableModule } from 'primeng/table'

import { SessionReportDto } from '@quezap/core/api-client'
import { Config } from '@quezap/core/services/config/config'
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
  private readonly config = inject(Config)
  private readonly http = inject(HttpClient)
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
    const apiUrl = this.config.appConfig.value().apiUrl
    this.http.get(`${apiUrl}/sessions/${this.sessionId()}/export/csv`, { responseType: 'blob' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `session-${this.report()?.sessionNumber || this.sessionId()}-resultats.csv`
          a.click()
          window.URL.revokeObjectURL(url)
        },
        error: () => {
          this.error.set('Échec du téléchargement du fichier CSV')
        },
      })
  }

  protected getSuccessBadgeSeverity(rate: number): 'success' | 'warn' | 'danger' {
    if (rate >= 70) return 'success'
    if (rate >= 40) return 'warn'
    return 'danger'
  }
}
