import { ChangeDetectionStrategy, Component } from '@angular/core'

import { NicknameChooser } from '../../components/nickname-chooser/nickname-chooser'

@Component({
  selector: 'quizz-lobby',
  imports: [
    NicknameChooser,
  ],
  templateUrl: './lobby.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Lobby {

}
