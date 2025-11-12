import { Component, computed, inject } from '@angular/core'

import { Image } from 'primeng/image'

import { Config } from '@quezap/core/services'

@Component({
  selector: 'quizz-footer',
  imports: [Image],
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
