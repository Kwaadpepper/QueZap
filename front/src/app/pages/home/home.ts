import { NgOptimizedImage } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core'
import { customError, Field, form, validate } from '@angular/forms/signals'
import { Router, RouterLink } from '@angular/router'

import { Button, ButtonDirective, ButtonLabel } from 'primeng/button'
import { Divider } from 'primeng/divider'
import { InputText } from 'primeng/inputtext'

import { Config, LayoutSettings } from '@quezap/core/services'
import { isValidSessionCode } from '@quezap/domain/models'
import { RegisterModal } from '@quezap/features/admin/account/components'
import { FieldError } from '@quezap/shared/directives'

@Component({
  selector: 'quizz-home',
  imports: [
    NgOptimizedImage,
    RouterLink,
    Divider,
    Button,
    InputText,
    ButtonDirective,
    ButtonLabel,
    RegisterModal,
    FieldError,
    Field,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  private readonly config = inject(Config)
  private readonly layout = inject(LayoutSettings)
  private readonly router = inject(Router)

  protected readonly appName = computed(() => this.config.appConfig.value().appName)

  protected readonly registerModalVisible = signal(false)

  // JOIN FORM
  protected readonly joinCode = signal('')

  protected readonly joinCodeForm = form(this.joinCode, (joinCode) => {
    validate(joinCode, ({ value }) => {
      return isValidSessionCode(value())
        ? []
        : customError({
            kind: 'invalid-value',
            message: 'Le code de session est invalide.',
          })
    })
  })

  protected onSubmitJoinForm() {
    const sessionCode = this.joinCode().trim()

    if (isValidSessionCode(sessionCode)) {
      this.router.navigate(['/quizz/join', sessionCode])
    }
  }

  ngOnInit(): void {
    this.layout.inContainer.set(false)
  }

  ngOnDestroy(): void {
    this.layout.inContainer.set(true)
  }
}
