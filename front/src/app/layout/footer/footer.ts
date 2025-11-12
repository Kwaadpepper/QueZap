import { Component, computed, inject } from '@angular/core'
import { RouterModule } from '@angular/router'

import { Image } from 'primeng/image'

import { Config } from '@quezap/core/services'

@Component({
  selector: 'quizz-footer',
  imports: [
    Image,
    RouterModule,
  ],
  templateUrl: './footer.html',
})
export class Footer {
  private readonly config = inject(Config)
  protected readonly footer = computed(() => {
    return {
      authorName: this.config.appConfig.value().authorName,
      authorEmail: this.config.appConfig.value().authorEmail,
    }
  })
}
