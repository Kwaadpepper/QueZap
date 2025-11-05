import { Component, computed, inject, model, signal } from '@angular/core'

import { ConfigService } from '@quezap/core/services/config'

@Component({
  selector: 'quizz-debugbar',
  imports: [],
  templateUrl: './debugbar.html',
})
export class Debugbar {
  private readonly config = inject(ConfigService)

  protected readonly asWebsite = signal<boolean>(true)
  protected readonly debug = computed(() => this.config.debug())

  public readonly websiteMode = model<boolean>()

  toggleWebsiteMode(): void {
    this.asWebsite.update(value => !value)
    this.websiteMode.set(this.asWebsite())
  }
}
