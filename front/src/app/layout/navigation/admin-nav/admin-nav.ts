import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { Image } from 'primeng/image'

@Component({
  selector: 'quizz-admin-nav',
  imports: [
    Image,
    RouterModule,
    ButtonDirective,
  ],
  templateUrl: './admin-nav.html',
})
export class AdminNav {

}
