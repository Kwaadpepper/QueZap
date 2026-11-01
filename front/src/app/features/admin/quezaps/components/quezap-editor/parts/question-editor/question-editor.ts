import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'

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

  protected readonly mediaFormat = signal<'STANDARD' | 'LARGE' | 'FULLSCREEN'>('LARGE')
  protected readonly backgroundTheme = signal<'CLASSIC' | 'DARK' | 'CLEAN'>('CLASSIC')

  protected readonly mediaClass = computed(() => {
    switch (this.mediaFormat()) {
      case 'STANDARD':
        return 'max-h-60'
      case 'FULLSCREEN':
        return 'max-h-[30rem] w-full'
      case 'LARGE':
      default:
        return 'max-h-80'
    }
  })

  protected readonly backgroundClass = computed(() => {
    switch (this.backgroundTheme()) {
      case 'DARK':
        return 'bg-gradient-to-br from-indigo-950 via-slate-900 to-slate-950 text-white'
      case 'CLEAN':
        return 'bg-slate-900 text-white'
      case 'CLASSIC':
      default:
        return 'quezap-theme-background'
    }
  })

  protected setMediaFormat(format: 'STANDARD' | 'LARGE' | 'FULLSCREEN') {
    this.mediaFormat.set(format)
  }

  protected setBackgroundTheme(theme: 'CLASSIC' | 'DARK' | 'CLEAN') {
    this.backgroundTheme.set(theme)
  }

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
