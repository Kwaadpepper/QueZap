import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Router } from '@angular/router'

import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { ChipModule } from 'primeng/chip'

import { QuezapWithTheme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

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

  readonly quezap = input.required<QuezapWithTheme>()
  readonly editable = input<boolean>(false)

  protected onQuezapEdit(): void {
    this.router.navigate(['/admin/quezaps', this.quezap().id, 'edit'])
  }
}
