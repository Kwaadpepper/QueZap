import { Component, computed, inject } from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorStore } from '../../../../../../stores'
import { QuestionEditorInput } from '../../question-editor'

@Component({
  selector: 'quizz-panel-question-actions',
  templateUrl: './panel-question-actions.html',
  imports: [
    ButtonModule,
    IconFacade,
  ],
})
export class PanelQuestionActions {
  private readonly confirmationService = inject(ConfirmationService)
  private readonly editorStore = inject(QuezapEditorStore)

  protected readonly question = computed<QuestionEditorInput>(() =>
    this.editorStore.selectedQuestion(),
  )

  protected readonly questionsCount = computed(() =>
    this.editorStore.questions().length,
  )

  protected onDuplicateQuestion() {
    this.editorStore.duplicateQuestionAtIdx(
      this.editorStore.selectionQuestionIdx(),
    )
  }

  protected onDeleteQuestion($event: Event) {
    this.confirmationService.confirm({
      target: $event.currentTarget ?? undefined,
      modal: true,
      header: 'Confirmer la suppression',
      message: 'Êtes-vous sûr de vouloir supprimer cette question ?',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      blockScroll: true,
      rejectButtonProps: {
        severity: 'primary',
        outlined: true,
      },
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.editorStore.deleteQuestionAtIdx(
          this.editorStore.selectionQuestionIdx(),
        )
      },
    })
  }
}
