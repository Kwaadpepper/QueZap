import { Component } from '@angular/core'
import { RouterModule } from '@angular/router'

import { ButtonDirective } from 'primeng/button'
import { Image } from 'primeng/image'

@Component({
  selector: 'quizz-site-nav',
  imports: [
    Image,
    RouterModule,
    ButtonDirective,
  ],
  templateUrl: './site-nav.html',
})
export class SiteNav {

}
