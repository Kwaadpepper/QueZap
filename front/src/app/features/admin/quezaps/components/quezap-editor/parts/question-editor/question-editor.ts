import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'

import { DividerModule } from 'primeng/divider'
import { FileSelectEvent, FileUploadModule } from 'primeng/fileupload'

import { PictureUrl, QuestionWithAnswers } from '@quezap/domain/models'

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
    TypeSelector,
    LimitSelector,
    PointsSelector,
    DividerModule,
    PhraseEditor,
    QuestionTimer,
    AnswerEditor,
    PanelQuestionActions,
    FileUploadModule,
  ],
  templateUrl: './question-editor.html',
  styleUrl: './question-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {
  private readonly editorStore = inject(QuezapEditorStore)

  protected readonly question = computed(() => this.editorStore.selectedQuestion())

  protected onUpload(event: FileSelectEvent) {
    const file = event.files[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const pictureUrl = (e.target?.result as string) as PictureUrl
        const q = this.question()
        this.editorStore.updateQuestionAtIdx(this.editorStore.selectionQuestionIdx(), {
          ...q,
          picture: pictureUrl,
        })
      }
      reader.readAsDataURL(file)
    }
  }

  protected removePicture() {
    const q = this.question()
    this.editorStore.updateQuestionAtIdx(this.editorStore.selectionQuestionIdx(), {
      ...q,
      picture: undefined,
    })
  }
}
