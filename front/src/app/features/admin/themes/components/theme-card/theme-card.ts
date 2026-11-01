import { ChangeDetectionStrategy, Component, inject, input, model } from '@angular/core'

import { ConfirmationService, MessageService } from 'primeng/api'
import { Button } from 'primeng/button'
import { Card } from 'primeng/card'
import { ConfirmPopupModule } from 'primeng/confirmpopup'

import { Theme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { THEME_SERVICE, ThemesEventsBus } from '../../services'
import { ThemeEditor } from '../theme-editor/theme-editor'

@Component({
  selector: 'quizz-theme-card',
  imports: [
    Card,
    Button,
    ConfirmPopupModule,
    ThemeEditor,
    IconFacade,
  ],
  providers: [ConfirmationService],
  templateUrl: './theme-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeCard {
  private readonly themeService = inject(THEME_SERVICE)
  private readonly themesEventBus = inject(ThemesEventsBus)
  private readonly confirmationService = inject(ConfirmationService)
  private readonly messageService = inject(MessageService)

  public readonly theme = input.required<Theme>()
  public readonly editable = input<boolean>(false)

  protected readonly editorIsVisible = model(false)

  protected onOpenEditor() {
    this.editorIsVisible.set(true)
  }

  protected onDeleteTheme($event: Event) {
    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      message: `Voulez-vous vraiment supprimer le thème "${this.theme().name}" ?`,
      accept: () => {
        this.themeService.delete(this.theme().id).subscribe((res) => {
          if (res.kind === 'success') {
            this.messageService.add({
              severity: 'success',
              summary: 'Thème supprimé',
              detail: 'Le thème a bien été supprimé',
              life: 5000,
            })
            this.themesEventBus.dispatchChanged(this.theme().id)
          }
          else {
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Échec de la suppression du thème',
            })
          }
        })
      },
    })
  }
}
