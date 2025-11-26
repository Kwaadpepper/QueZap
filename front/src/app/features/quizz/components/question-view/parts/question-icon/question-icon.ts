import { HttpClient } from '@angular/common/http'
import {
  ChangeDetectionStrategy, Component,
  computed,
  DestroyRef,
  effect,
  inject, input,
  signal,
  ViewChild,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { DomSanitizer, SafeHtml } from '@angular/platform-browser'

import { tap } from 'rxjs'

export enum QuestionIconType {
  Circle = 'circle',
  Cross = 'cross',
  Diamond = 'diamond',
  Hexagon = 'hexagon',
  Pentagon = 'pentagon',
  Square = 'square',
  Star = 'star',
  Triangle = 'triangle',
}

@Component({
  selector: 'quizz-question-icon',
  imports: [],
  template: `
    <span #spanElement class="question-icon" [class.checked]="checked()">
      <svg [innerHTML]="svgContent()"></svg>
    </span>
  `,
  styleUrl: './question-icon.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionIcon {
  readonly #iconsUrlPath = 'images/answers'
  private readonly sanitizer = inject(DomSanitizer)
  private readonly httpClient = inject(HttpClient)
  private readonly destroyRef = inject(DestroyRef)

  readonly form = input.required<QuestionIconType>()
  readonly checked = input.required<boolean>()

  private readonly src = computed(() => `${this.#iconsUrlPath}/${this.form()}.svg`)
  private readonly svgCache = new Map<string, SafeHtml>()
  protected readonly svgContent = signal<SafeHtml>('')

  @ViewChild('spanElement')
  protected readonly spanElement!: { nativeElement: HTMLSpanElement }

  constructor() {
    effect(() => {
      const imageSourceUrl = this.src()
      const cachedSvg = this.svgCache.get(imageSourceUrl)

      if (cachedSvg) {
        this.svgContent.set(cachedSvg)
        this.breakInjectedSvgVirtualEncapsulation()
        return
      }

      this.httpClient.get(imageSourceUrl, { responseType: 'text' })
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          tap((svgContent) => {
            const safeSvg = this.sanitizer.bypassSecurityTrustHtml(svgContent)
            this.svgCache.set(imageSourceUrl, safeSvg)
            this.svgContent.set(safeSvg)
            this.breakInjectedSvgVirtualEncapsulation()
          }),
        ).subscribe()
    })
  }

  private breakInjectedSvgVirtualEncapsulation(): void {
    setTimeout(() => {
      if (!this.spanElement?.nativeElement) {
        return
      }

      const spanEl = this.spanElement.nativeElement

      console.debug(spanEl)

      const svgParentElement = spanEl.querySelector('svg')
      const svgInjectChildElement = spanEl.querySelector('svg svg')
      if (!svgParentElement || !svgInjectChildElement) {
        return
      }

      let encapsulationAttribute: string | null = null
      for (const attr of spanEl.attributes) {
        if (attr.name.startsWith('_ngcontent-')) {
          encapsulationAttribute = attr.name
          break
        }
      }
      // Copy attributes from injected SVG to parent SVG
      for (const attrName of svgInjectChildElement.getAttributeNames()) {
        const attrValue = svgInjectChildElement.getAttribute(attrName)
        if (attrValue) {
          svgParentElement.setAttribute(attrName, attrValue)
        }
      }

      // Move Child Nodes from injected SVG to parent SVG
      while (svgInjectChildElement.firstChild) {
        const svgChildEl = svgInjectChildElement.firstChild
        svgParentElement.appendChild(svgChildEl)
        if (!encapsulationAttribute) {
          continue
        }
        if (
          svgChildEl instanceof SVGPathElement
          || svgChildEl instanceof SVGPolygonElement
          || svgChildEl instanceof SVGCircleElement
          || svgChildEl instanceof SVGEllipseElement
          || svgChildEl instanceof SVGRectElement
        ) {
          svgChildEl.setAttribute(encapsulationAttribute, '')
        }
      }

      // Remove injected SVG element
      svgInjectChildElement.remove()
    }, 0)
  }
}
