import { ChangeDetectionStrategy, Component, input } from '@angular/core'

@Component({
  selector: 'quizz-submission-feedback',
  imports: [],
  templateUrl: './submission-feedback.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubmissionFeedback {
  readonly error = input<string | null>(null)
  readonly isSubmitting = input<boolean>(false)
  readonly hasSubmitted = input<boolean>(false)
  readonly submittedMessage = input<string>('Choix enregistré !')
  readonly hintMessage = input<string>('Modifiable tant que le chrono tourne')
}
