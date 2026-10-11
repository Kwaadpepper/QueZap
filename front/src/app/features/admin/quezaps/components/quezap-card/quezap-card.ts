import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { ChipModule } from 'primeng/chip'

import { QuezapWithTheme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

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
  private readonly messageService = inject(MessageService)
  private readonly quezapPage = inject(QuezapPageStore)

  readonly quezap = input.required<QuezapWithTheme>()
  readonly editable = input<boolean>(false)

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
