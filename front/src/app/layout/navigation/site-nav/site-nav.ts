import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonModule } from 'primeng/button'

@Component({
  selector: 'quizz-site-nav',
  imports: [
    RouterModule,
    ButtonModule,
  ],
  templateUrl: './site-nav.html',
})
export class SiteNav {

}
