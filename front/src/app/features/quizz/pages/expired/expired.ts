import { ChangeDetectionStrategy, Component } from '@angular/core'

import { JoinForm } from '../../components'

@Component({
  selector: 'quizz-expired',
  imports: [JoinForm],
  templateUrl: './expired.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Expired {

}
