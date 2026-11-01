import {
  ChangeDetectionStrategy, Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core'
import { Router } from '@angular/router'

import { ConfirmationService, MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { ConfirmDialogModule } from 'primeng/confirmdialog'

import { ValidationError } from '@quezap/core/errors'
import { QuestionType, QuestionTypeFrom, QuezapId, QuezapWithQuestionsAndAnswers } from '@quezap/domain/models'
import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

import { SESSION_SERVICE } from '../../../sessions/services/session'
import { QuezapEditorStore } from '../../stores'

import { QuestionEditor, QuestionListView, TitleEditor } from './parts'

export type QuezapEditorInput
  = | QuezapWithQuestionsAndAnswers
    | (Omit<QuezapWithQuestionsAndAnswers, 'id'> & { id?: QuezapId })

@Component({
  selector: 'quizz-quezap-editor',
  imports: [
    QuestionListView,
    QuestionEditor,
    TitleEditor,
    ButtonModule,
    ConfirmDialogModule,
    IconFacade,
  ],
  providers: [
    ConfirmationService,
    QuezapEditorStore,
  ],
  templateUrl: './quezap-editor.html',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuezapEditor {
  private readonly editorStore = inject(QuezapEditorStore)
  private readonly confirmation = inject(ConfirmationService)
  private readonly message = inject(MessageService)
  private readonly router = inject(Router)
  private readonly sessionService = inject(SESSION_SERVICE)

  readonly quezap = input<QuezapEditorInput>()
  private readonly _quezap = signal<QuezapEditorInput>({
    title: '',
    description: '',
    questionWithAnswersAndResponses: [
      // * Editor requires minimum 1 question
      this.getInitialQuestion(),
    ],
  })

  readonly closeEvent = output<void>()

  protected readonly savedQuezapId = computed(() => this.editorStore.quezap().id)
  protected readonly persisting = computed(() => this.editorStore.persisting())
  protected readonly isDirty = computed(() => this.editorStore.isDirty())
  protected readonly hasInvalidAnswers = computed(() => this.editorStore.hasInvalidAnswers())
  protected readonly launchingSession = signal(false)

  constructor() {
    this.editorStore.setQuezap(this._quezap())

    effect(() => {
      const newInput = this.quezap()

      if (newInput !== undefined) {
        const inputQuestions = newInput.questionWithAnswersAndResponses
        this._quezap.set({
          ...newInput,
          questionWithAnswersAndResponses:
            inputQuestions.length
              ? inputQuestions
              : [this.getInitialQuestion()],
        })
      }
    }, { debugName: 'Input changed' })

    effect(() => {
      const quezap = this._quezap()
      this.editorStore.setQuezap(quezap)
    }, { debugName: 'Internal Quezap changed' })
  }

  protected onSaveQuezap() {
    this.editorStore.persist()
      .then(() => this.message.add({
        summary: 'Enregistré !',
        severity: 'success',
      }))
      .catch((err) => {
        if (err instanceof ValidationError) {
          this.message.add({
            summary: 'Validation',
            detail: err.message || 'Vous devez corriger votre questionnaire avant qu\'il soit sauvegardé',
            severity: 'warn',
          })
          return
        }

        throw new Error('Erreur lors de l\'enregistrement du questionnaire', err)
      })
  }

  protected onLaunchSession(): void {
    const q = this.editorStore.quezap()
    const id = q.id
    if (!id) return

    const launch = (quezapId: string) => {
      this.launchingSession.set(true)
      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      const name = `${q.title || 'Session'} - ${timeStr}`
      this.sessionService.createSession(quezapId, name).subscribe((output) => {
        this.launchingSession.set(false)
        if (output.kind === 'success') {
          this.message.add({
            summary: 'Session lancée !',
            detail: `Code PIN : ${output.result.code ?? ''}`,
            severity: 'success',
          })
          const url = this.router.serializeUrl(
            this.router.createUrlTree(['/projector', output.result.id]),
          )
          const win = window.open(url, '_blank')
          if (!win) {
            this.router.navigate(['/projector', output.result.id])
          }
        }
        else {
          this.message.add({
            summary: 'Erreur',
            detail: 'Impossible de créer la session',
            severity: 'error',
          })
        }
      })
    }

    if (this.isDirty()) {
      this.editorStore.persist().then(saved => launch(saved.id)).catch(() => void 0)
    }
    else {
      launch(id)
    }
  }

  protected onCloseEditor() {
    if (this.isDirty()) {
      this.confirmation.confirm({
        message: 'Voulez-vous quitter sans enregistrer les modifications ?',
        header: 'Confirmation',
        acceptButtonStyleClass: 'p-button-danger p-button-text',
        acceptLabel: 'Oui',
        rejectLabel: 'Non',
        accept: () => {
          this.closeEvent.emit()
        },
      })
      return
    }

    this.closeEvent.emit()
  }

  private getInitialQuestion() {
    return QuestionTypeFrom(QuestionType.Quizz).getNewWithAnswers()
  }
}
