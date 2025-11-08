import { Component, computed, inject, OnDestroy, OnInit } from '@angular/core'

import { AnimateOnScrollModule } from 'primeng/animateonscroll'
import { Divider } from 'primeng/divider'
import { Image } from 'primeng/image'

import { Config, LayoutSettings } from '@quezap/core/services'

@Component({
  selector: 'quizz-home',
  imports: [
    AnimateOnScrollModule,
    Image,
    Divider,
  ],
  templateUrl: './home.html',
})
export class Home implements OnInit, OnDestroy {
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)

  protected readonly appName = computed(() => this.config.appConfig.value().appName)

  ngOnInit(): void {
    this.layout.inContainer.set(false)
  }

  ngOnDestroy(): void {
    this.layout.inContainer.set(true)
  }
}
