import { NgOptimizedImage } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core'
import { RouterLink } from '@angular/router'

import { AnimateOnScrollModule } from 'primeng/animateonscroll'
import { Button, ButtonDirective, ButtonLabel } from 'primeng/button'
import { Divider } from 'primeng/divider'
import { InputText } from 'primeng/inputtext'

import { Config, LayoutSettings } from '@quezap/core/services'

@Component({
  selector: 'quizz-home',
  imports: [
    NgOptimizedImage,
    RouterLink,
    AnimateOnScrollModule,
    Divider,
    Button,
    InputText,
    ButtonDirective,
    ButtonLabel,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)

  protected readonly appName = computed(() => this.config.appConfig.value().appName)

  // Code pour rejoindre une partie existante via QR ou code manuel
  protected readonly joinCode = signal('')
  protected readonly isJoinCodeValid = computed(() => /^\w{4,10}$/i.test(this.joinCode()))

  protected onJoinGame() {
    if (!this.isJoinCodeValid()) return
    console.log(`Rejoindre la partie avec le code: ${this.joinCode()}`)
  }

  protected onJoinCodeInput(ev: Event) {
    const value = (ev.target as HTMLInputElement | null)?.value ?? ''
    this.joinCode.set(value.trim())
  }

  protected onSubmitJoinForm(ev: Event) {
    ev.preventDefault()
    this.onJoinGame()
  }

  ngOnInit(): void {
    this.layout.inContainer.set(false)
  }

  ngOnDestroy(): void {
    this.layout.inContainer.set(true)
  }
}
