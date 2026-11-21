import { NgOptimizedImage } from '@angular/common'
import {
  ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal,
} from '@angular/core'
import { RouterLink } from '@angular/router'

import { ButtonModule } from '@openng/optimus-ui/button'
import { Divider } from '@openng/optimus-ui/divider'

import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'
import { RegisterModal } from '@quezap/features/admin/account/components/register-modal/register-modal'
import { JoinForm } from '@quezap/features/quizz/components/join-form/join-form'
import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

@Component({
  selector: 'quizz-home',
  imports: [
    NgOptimizedImage,
    RouterLink,
    Divider,
    ButtonModule,
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
  private readonly authStore = inject(AuthenticatedUserStore)

  protected readonly appName = computed(() => this.config.appConfig.value().appName)
  protected readonly isLoggedIn = computed(() => this.authStore.isLoggedIn())

  protected readonly registerModalVisible = signal(false)

  ngOnInit(): void {
    this.layout.inContainer.set(false)
  }

  ngOnDestroy(): void {
    this.layout.inContainer.set(true)
  }
}
