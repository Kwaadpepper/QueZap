import { ChangeDetectionStrategy, Component } from '@angular/core'

import { MessageModule } from 'primeng/message'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { JoinForm } from '../../components/join-form/join-form'

@Component({
  selector: 'quizz-ended',
  imports: [
    MessageModule,
    JoinForm,
    IconFacade,
  ],
  templateUrl: './ended.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ended {

}
