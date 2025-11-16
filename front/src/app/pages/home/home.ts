import { NgOptimizedImage } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core'
import { RouterLink } from '@angular/router'

import { ButtonDirective, ButtonLabel } from 'primeng/button'
import { Divider } from 'primeng/divider'

import { Config, LayoutSettings } from '@quezap/core/services'
import { RegisterModal } from '@quezap/features/admin/account/components'
import { JoinForm } from '@quezap/features/quizz/components'

@Component({
  selector: 'quizz-home',
  imports: [
    NgOptimizedImage,
    RouterLink,
    Divider,
    ButtonDirective,
    ButtonLabel,
    RegisterModal,
    JoinForm,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)

  protected readonly appName = computed(() => this.config.appConfig.value().appName)

  protected readonly registerModalVisible = signal(false)

  ngOnInit(): void {
    this.layout.inContainer.set(false)
  }

  ngOnDestroy(): void {
    this.layout.inContainer.set(true)
  }
}
