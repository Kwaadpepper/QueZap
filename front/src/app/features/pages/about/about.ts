import { Component } from '@angular/core'

import { MarkdownViewer } from '@quezap/core/components'

@Component({
  selector: 'quizz-about',
  imports: [MarkdownViewer],
  templateUrl: './about.html',
})
export class About {
  protected readonly url = 'markdown/how-it-is-working.md'
}
