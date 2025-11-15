import { Component, input, model, OnInit, output } from '@angular/core'

import { Button } from 'primeng/button'
import { Card } from 'primeng/card'

import { Theme } from '@quezap/domain/models'

import { ThemeEditor } from '../theme-editor/theme-editor'

@Component({
  selector: 'quizz-theme-card',
  imports: [
    Card,
    Button,
    ThemeEditor,
  ],
  templateUrl: './theme-card.html',
})
export class ThemeCard implements OnInit {
  public readonly theme = input.required<Theme>()
  public readonly themeUpdated = output<Theme>()

  protected readonly name = model<string>()

  protected readonly editorIsVisible = model(false)

  public readonly editable = input<boolean>(false)

  ngOnInit() {
    this.name.set(this.theme().name)
  }

  protected onOpenEditor() {
    this.editorIsVisible.set(true)
  }

  protected onThemeUpdated(updatedTheme: Theme) {
    this.name.set(updatedTheme.name)
    this.themeUpdated.emit({
      uuid: updatedTheme.uuid,
      name: updatedTheme.name,
    })
  }
}
