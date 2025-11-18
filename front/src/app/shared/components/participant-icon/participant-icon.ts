import { ChangeDetectionStrategy, Component, input } from '@angular/core'

@Component({
  selector: 'quizz-participant-icon',
  imports: [],
  templateUrl: './participant-icon.html',
  styleUrl: './participant-icon.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ParticipantIcon {
  readonly leafColor = input.required()
  readonly backgroundColor = input.required()
}
