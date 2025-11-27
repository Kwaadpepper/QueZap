import { Component, computed, inject, input } from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmPopupModule } from 'primeng/confirmpopup'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorContainer } from '../../../../editor-container'

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
  private readonly editorContainer = inject(QuezapEditorContainer)

  readonly index = input.required<number>()
  readonly selected = computed(() => this.editorContainer.selectionQuestionIdx() === this.index())
  protected readonly questions = computed(() => this.editorContainer.questions())

  protected onDuplicateQuestion(index: number) {
    this.editorContainer.duplicateQuestionAtIdx(index)
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
        this.editorContainer.deleteQuestionAtIdx(index)
      },
    })
  }
}
