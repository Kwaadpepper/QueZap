import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonModule } from '@openng/optimus-ui/button'
import { DrawerModule } from '@openng/optimus-ui/drawer'

@Component({
  selector: 'quizz-admin-nav',
  imports: [
    RouterModule,
    ButtonModule,
    DrawerModule,
  ],
  templateUrl: './admin-nav.html',
})
export class AdminNav {
}
