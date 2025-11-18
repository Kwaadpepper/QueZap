import { ChangeDetectionStrategy, Component } from '@angular/core'

import { NicknameChooser, ParticipantList } from '../../components'

@Component({
  selector: 'quizz-lobby',
  imports: [
    NicknameChooser,
    ParticipantList,
  ],
  templateUrl: './lobby.html',
  styleUrl: './lobby.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Lobby {
}
