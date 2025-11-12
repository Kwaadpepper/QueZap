import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { DrawerModule } from 'primeng/drawer'

@Component({
  selector: 'quizz-admin-nav',
  imports: [
    RouterModule,
    ButtonDirective,
    DrawerModule,
  ],
  templateUrl: './admin-nav.html',
})
export class AdminNav {
}
