import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonDirective } from 'primeng/button'

@Component({
  selector: 'quizz-site-nav',
  imports: [
    RouterModule,
    ButtonDirective,
  ],
  templateUrl: './site-nav.html',
})
export class SiteNav {

}
