import { HttpClient } from '@angular/common/http'
import { Component, inject, input } from '@angular/core'

import { MarkdownComponent, MarkdownService, MARKED_OPTIONS, MERMAID_OPTIONS, provideMarkdown, SANITIZE } from 'ngx-markdown'

@Component({
  selector: 'quizz-markdow-viewer',
  imports: [
    MarkdownComponent,
  ],
  providers: [
    provideMarkdown({
      loader: HttpClient,
      markedOptions: {
        provide: MARKED_OPTIONS,
        useValue: {
          gfm: true,
          breaks: false,
          pedantic: false,
        },
      },
      mermaidOptions: {
        provide: MERMAID_OPTIONS,
        useValue: {
          darkMode: true,
          look: 'handDrawn',
        },
      },
      sanitize: {
        provide: SANITIZE,
        useValue: (html: string) => html,
      },
    }),
  ],
  templateUrl: './markdow-viewer.html',
})
export class MarkdownViewer {
  public readonly url = input<string>('')

  private readonly markdownService: MarkdownService = inject(MarkdownService)
}
