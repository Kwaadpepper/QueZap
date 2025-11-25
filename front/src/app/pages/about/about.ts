import { Component } from '@angular/core'

import { MarkdownViewer } from '@quezap/shared/components/markdown-viewer/markdow-viewer'

@Component({
  selector: 'quizz-about',
  imports: [MarkdownViewer],
  templateUrl: './about.html',
})
export class About {
  protected readonly url = 'markdown/how-it-is-working.md'
}
