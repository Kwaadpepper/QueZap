import {
  ChangeDetectionStrategy, Component, computed, inject, input, signal,
} from '@angular/core'
import { Router } from '@angular/router'

import { ConfirmationService, MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { ChipModule } from 'primeng/chip'
import { ConfirmPopupModule } from 'primeng/confirmpopup'

import { QuezapWithTheme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { SESSION_SERVICE } from '../../../sessions/services/session'
import { QUEZAP_SERVICE } from '../../services/quezap'
import { QuezapPageStore } from '../../stores'

@Component({
  selector: 'quizz-quezap-card',
  imports: [
    CardModule,
    ChipModule,
    ConfirmPopupModule,
    IconFacade,
    ButtonModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './quezap-card.html',
  styleUrl: './quezap-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapCard {
  private readonly router = inject(Router)
  private readonly quezapService = inject(QUEZAP_SERVICE)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly messageService = inject(MessageService)
  private readonly confirmationService = inject(ConfirmationService)
  private readonly quezapPage = inject(QuezapPageStore)
  private readonly authStore = inject(AuthenticatedUserStore)

  readonly quezap = input.required<QuezapWithTheme>()
  readonly editable = input<boolean>(false)
  protected readonly canEdit = computed(() => {
    if (!this.editable()) {
      return false
    }
    const authorId = this.quezap().authorId
    const currentUserId = this.authStore.currentUser()?.id
    if (!authorId || !currentUserId) {
      return true
    }
    return authorId === currentUserId
  })

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
        if (this.quezapPage.pageInfo().page === 1) {
          this.quezapPage.reload()
        }
        else {
          this.quezapPage.setPagination({ page: 1 })
        }
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

  protected onQuezapDelete($event: Event): void {
    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      message: `Voulez-vous vraiment supprimer le quiz "${this.quezap().title}" ?`,
      accept: () => {
        this.quezapService.deleteQuezap(this.quezap().id).subscribe((res) => {
          if (res.kind === 'success') {
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Quiz supprimé avec succès',
            })
            this.quezapPage.reload()
          }
          else {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec de la suppression du quiz',
            })
          }
        })
      },
    })
  }
}
