import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

import { Button, ButtonIcon } from 'primeng/button'
import { Card } from 'primeng/card'

import { Theme } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { ThemeEditor } from '../theme-editor/theme-editor'

@Component({
  selector: 'quizz-theme-card',
  imports: [
    Card,
    Button,
    ThemeEditor,
    ButtonIcon,
    IconFacade,
  ],
  templateUrl: './theme-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeCard {
  public readonly theme = input.required<Theme>()
  public readonly editable = input<boolean>(false)

  protected readonly editorIsVisible = model(false)

  protected onOpenEditor() {
    this.editorIsVisible.set(true)
  }
}
