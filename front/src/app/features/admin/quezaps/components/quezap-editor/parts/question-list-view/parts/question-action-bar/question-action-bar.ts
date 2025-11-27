import { Component, computed, inject, input } from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmPopupModule } from 'primeng/confirmpopup'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorStore } from '../../../../../../stores'

@Component({
  selector: 'quizz-question-action-bar',
  imports: [
    IconFacade,
    ConfirmPopupModule,
    ButtonModule,
  ],
  templateUrl: './question-action-bar.html',
})
export class QuestionActionBar {
  private readonly confirmationService = inject(ConfirmationService)
  private readonly editorStore = inject(QuezapEditorStore)

  readonly index = input.required<number>()
  readonly selected = computed(() => this.editorStore.selectionQuestionIdx() === this.index())
  protected readonly questions = computed(() => this.editorStore.questions())

  protected onDuplicateQuestion(index: number) {
    this.editorStore.duplicateQuestionAtIdx(index)
  }

  protected onDeleteQuestion($event: Event, index: number) {
    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      blockScroll: true,
      rejectButtonProps: {
        severity: 'primary',
        outlined: true,
      },
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.editorStore.deleteQuestionAtIdx(index)
      },
    })
  }
}
