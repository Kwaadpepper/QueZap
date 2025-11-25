import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonModule } from 'primeng/button'
import { DrawerModule } from 'primeng/drawer'

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
