import { Component, computed, input } from '@angular/core'

import { Button } from 'primeng/button'
import { Card } from 'primeng/card'

import { Theme } from '@quezap/domain/models'

@Component({
  selector: 'quizz-theme-card',
  imports: [
    Card,
    Button,
  ],
  templateUrl: './theme-card.html',
})
export class ThemeCard {
  public readonly theme = input.required<Theme>()

  protected readonly id = computed(() => this.theme().id)
  protected readonly name = computed(() => this.theme().name)

  protected onClick() {
    console.log(`Theme card clicked: ${this.name()}`)
  }
}
