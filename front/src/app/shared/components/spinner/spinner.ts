import { ChangeDetectionStrategy, Component } from '@angular/core'

import { ProgressSpinner } from '@openng/optimus-ui/progressspinner'

@Component({
  selector: 'quizz-spinner',
  imports: [ProgressSpinner],
  templateUrl: './spinner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Spinner {

}
