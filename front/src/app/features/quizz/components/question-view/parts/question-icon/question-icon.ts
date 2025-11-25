import { HttpClient } from '@angular/common/http'
import {
  ChangeDetectionStrategy, Component,
  DestroyRef,
  effect,
  inject, input,
  signal,
  ViewEncapsulation,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { DomSanitizer, SafeHtml } from '@angular/platform-browser'

import { tap } from 'rxjs'

@Component({
  selector: 'quizz-question-icon',
  imports: [],
  template: `<span class="question-icon" [innerHTML]="svgContent()"></span>`,
  styles: '.question-icon svg { width: 100% !important; height: auto !important; }',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: '' },
})
export class QuestionIcon {
  private readonly sanitizer = inject(DomSanitizer)
  private readonly httpClient = inject(HttpClient)

  protected readonly svgContent = signal<SafeHtml>('')

  readonly src = input.required<string>()

  private readonly destroyRef = inject(DestroyRef)
  private readonly svgCache = new Map<string, SafeHtml>()

  constructor() {
    effect(() => {
      const imageSourceUrl = this.src()
      this.httpClient.get(imageSourceUrl, { responseType: 'text' })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          tap((svgContent) => {
            const safeSvg = this.sanitizer.bypassSecurityTrustHtml(svgContent) ?? ''
            this.svgContent.set(safeSvg)
          }),
        )
        .subscribe()
    })
  }
}
