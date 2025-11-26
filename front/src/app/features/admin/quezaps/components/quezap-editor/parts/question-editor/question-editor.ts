import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { ConfirmationService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmPopup } from 'primeng/confirmpopup'
import { Divider } from 'primeng/divider'

import { QuestionTypeFrom, QuestionWithAnswers } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { QuezapEditorContainer } from '../../editor-container'

import { LimitSelector, PhraseEditor, QuestionTimer, TypeSelector } from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [
    TypeSelector,
    LimitSelector,
    Divider,
    PhraseEditor,
    QuestionTimer,
    ButtonModule,
    IconFacade,
    ConfirmPopup,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
  protected readonly QuestionTypeFrom = QuestionTypeFrom
  private readonly confirmationService = inject(ConfirmationService)
  private readonly editorContainer = inject(QuezapEditorContainer)

  protected readonly questionsCount = computed(() =>
    this.editorContainer.questions().length,
  )

  protected readonly question = computed<QuestionEditorInput>(() =>
    this.editorContainer.selectedQuestion(),
  )

  protected onDuplicateQuestion() {
    this.editorContainer.duplicateQuestionAtIdx(
      this.editorContainer.selectionQuestionIdx(),
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
        this.editorContainer.deleteQuestionAtIdx(
          this.editorContainer.selectionQuestionIdx(),
        )
      },
    })
  }
}
