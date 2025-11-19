import { ChangeDetectionStrategy, Component } from '@angular/core'

import { Message } from 'primeng/message'

import { JoinForm } from '../../components'

@Component({
  selector: 'quizz-ended',
  imports: [
    Message,
    JoinForm,
  ],
  templateUrl: './ended.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ended {

}
