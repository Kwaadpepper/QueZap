import { Component } from '@angular/core'

import { MarkdownViewer } from '@quezap/core/components'

@Component({
  selector: 'quizz-licence',
  imports: [MarkdownViewer],
  templateUrl: './licence.html',
})
export class Licence {
  protected readonly url = 'markdown/LICENCE.md'
}
