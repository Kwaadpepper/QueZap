import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { ChipModule } from 'primeng/chip'

import { QuezapWithTheme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { SESSION_SERVICE } from '../../../sessions/services/session'
import { QUEZAP_SERVICE } from '../../services/quezap'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-card',
  imports: [
    CardModule,
    ChipModule,
    IconFacade,
    ButtonModule,
  ],
  templateUrl: './quezap-card.html',
  styleUrl: './quezap-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapCard {
  private readonly router = inject(Router)
  private readonly quezapService = inject(QUEZAP_SERVICE)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly messageService = inject(MessageService)
  private readonly quezapPage = inject(QuezapPageStore)

  readonly quezap = input.required<QuezapWithTheme>()
  readonly editable = input<boolean>(false)
  protected readonly launchingSession = signal(false)

  protected onQuezapLaunch(): void {
    this.launchingSession.set(true)
    const q = this.quezap()
    const name = `${q.title} - ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
    this.sessionService.createSession(q.id, name).subscribe((res) => {
      this.launchingSession.set(false)
      if (res.kind === 'success') {
        this.messageService.add({
          severity: 'success',
          summary: 'Session lancée !',
          detail: `Code PIN : ${res.result.code ?? ''}`,
        })
        const url = this.router.serializeUrl(
          this.router.createUrlTree(['/admin/sessions', res.result.id, 'present']),
        )
        const win = window.open(url, '_blank')
        if (!win) {
          this.router.navigate(['/admin/sessions', res.result.id, 'present'])
        }
      }
      else {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du lancement de la session',
        })
      }
    })
  }

  protected onQuezapEdit(): void {
    this.router.navigate(['/admin/quezaps', this.quezap().id, 'edit'])
  }

  protected onQuezapDuplicate(): void {
    this.quezapService.duplicateQuezap(this.quezap().id).subscribe((res) => {
      if (res.kind === 'success') {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Quezap dupliqué avec succès',
        })
        this.quezapPage.setPagination({ page: 1 })
      }
      else {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec de la duplication du Quezap',
        })
      }
    })
  }
}
