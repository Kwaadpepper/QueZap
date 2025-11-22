import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { Button } from 'primeng/button'
import { Card } from 'primeng/card'

import { Quezap } from '@quezap/domain/models'

@Component({
  selector: 'quizz-quezap-card',
  imports: [
    Card,
    Button,
  ],
  templateUrl: './quezap-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapCard {
  public readonly quezap = input.required<Quezap>()
}
