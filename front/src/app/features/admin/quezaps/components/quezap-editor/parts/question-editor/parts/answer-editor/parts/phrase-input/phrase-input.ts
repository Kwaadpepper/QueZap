import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'

import { TextareaModule } from 'primeng/textarea'

@Component({
  selector: 'quizz-phrase-input',
  templateUrl: './phrase-input.html',
  styleUrl: './phrase-input.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TextareaModule],
})
export class PhraseInput {
  readonly index = input.required<number>()
  readonly phrase = input.required<string>()
  readonly readonly = input<boolean>(false)
  readonly phraseChanged = output<string>()

  protected onPhraseChanged(newPhrase: string) {
    this.phraseChanged.emit(
      this.stripNewLines(newPhrase),
    )
  }

  private stripNewLines(text: string): string {
    return text.replaceAll(/[\r\n]+/g, ' ')
  }
}
