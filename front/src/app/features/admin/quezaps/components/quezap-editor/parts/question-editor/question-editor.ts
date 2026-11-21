import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormsModule } from '@angular/forms'

import { MessageService } from '@openng/optimus-ui/api'
import { DividerModule } from '@openng/optimus-ui/divider'
import { FileSelectEvent, FileUploadModule } from '@openng/optimus-ui/fileupload'
import { Select } from '@openng/optimus-ui/select'

import { PictureUploadService } from '@quezap/core/services/picture-upload/picture-upload'
import { isSuccess } from '@quezap/core/types'
import { QuestionWithAnswers, QuestionWithAnswersAndResponses, Theme } from '@quezap/domain/models'
import { THEME_SERVICE } from '@quezap/features/admin/themes/services/theme'

import { QuezapEditorStore } from '../../../../stores'

import {
  AnswerEditor, LimitSelector,
  PanelQuestionActions,
  PhraseEditor, PointsSelector, QuestionTimer, TypeSelector,
} from './parts'

export type QuestionEditorInput = Omit<QuestionWithAnswers, 'id'>

@Component({
  selector: 'quizz-question-editor',
  imports: [
    FormsModule,
    TypeSelector,
    LimitSelector,
    PointsSelector,
    DividerModule,
    PhraseEditor,
    QuestionTimer,
    AnswerEditor,
    PanelQuestionActions,
    FileUploadModule,
    Select,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor implements OnInit {
  private readonly editorStore = inject(QuezapEditorStore)
  private readonly pictureUploadService = inject(PictureUploadService)
  private readonly messageService = inject(MessageService, { optional: true })
  private readonly themeService = inject(THEME_SERVICE, { optional: true })
  private readonly destroyRef = inject(DestroyRef)

  protected readonly question = computed(() =>
    this.editorStore.selectedQuestion() as QuestionWithAnswersAndResponses & { themeId?: string },
  )

  protected readonly isUploading = signal(false)
  protected readonly themes = signal<Theme[]>([])

  ngOnInit(): void {
    if (!this.themeService) {
      return
    }
    this.themeService.getThemePage({ page: 1, pageSize: 50 }).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((output) => {
      if (isSuccess(output)) {
        this.themes.set(output.result.data)
      }
    })
  }

  protected onThemeChange(themeId: string | null): void {
    const q = this.question()
    this.editorStore.updateQuestionAtIdx(this.editorStore.selectionQuestionIdx(), {
      ...q,
      themeId: themeId ?? undefined,
    } as QuestionWithAnswersAndResponses & { themeId?: string })
  }

  protected onUpload(event: FileSelectEvent) {
    const file = event.files[0]
    if (!file) {
      return
    }

    this.isUploading.set(true)
    this.pictureUploadService.upload(file).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (output) => {
        this.isUploading.set(false)
        if (isSuccess(output)) {
          const q = this.question()
          this.editorStore.updateQuestionAtIdx(this.editorStore.selectionQuestionIdx(), {
            ...q,
            picture: output.result.pictureUrl,
          })
        }
        else {
          this.messageService?.add({
            severity: 'error',
            summary: 'Erreur d’upload',
            detail: output.error.message || 'Impossible de téléverser l’image',
          })
        }
      },
      error: () => {
        this.isUploading.set(false)
        this.messageService?.add({
          severity: 'error',
          summary: 'Erreur d’upload',
          detail: 'Une erreur inattendue est survenue lors du téléversement',
        })
      },
    })
  }

  protected removePicture() {
    const q = this.question()
    this.editorStore.updateQuestionAtIdx(this.editorStore.selectionQuestionIdx(), {
      ...q,
      picture: undefined,
    })
  }
}
