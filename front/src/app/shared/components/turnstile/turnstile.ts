import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, DOCUMENT, effect, ElementRef, inject, output, viewChild,
} from '@angular/core'

import { Config } from '@quezap/core/services/config/config'

interface TurnstileRenderOptions {
  'sitekey': string
  'theme'?: 'auto' | 'light' | 'dark'
  'callback'?: (token: string) => void
  'expired-callback'?: () => void
  'error-callback'?: () => void
}

interface TurnstileApi {
  render: (container: HTMLElement, options: TurnstileRenderOptions) => string
  reset: (widgetId?: string) => void
  remove: (widgetId?: string) => void
}

type WindowWithTurnstile = Window & { turnstile?: TurnstileApi }

const TURNSTILE_SCRIPT_ID = 'cf-turnstile-script'
const TURNSTILE_SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

@Component({
  selector: 'quizz-turnstile',
  template: `
    @if (enabled()) {
      <div class="my-2 flex justify-center">
        <div #container></div>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TurnstileWidget {
  private readonly config = inject(Config)
  private readonly document = inject(DOCUMENT)
  private readonly destroyRef = inject(DestroyRef)

  private readonly container = viewChild<ElementRef<HTMLElement>>('container')
  private widgetId: string | null = null

  public readonly tokenChange = output<string | null>()

  public readonly siteKey = computed(() => this.config.appConfig?.value().turnstileSiteKey ?? '')
  public readonly enabled = computed(() => this.siteKey().length > 0)

  constructor() {
    effect(() => {
      const key = this.siteKey()
      const containerRef = this.container()
      if (!key || !containerRef) {
        return
      }
      this.ensureScriptAndRender(containerRef.nativeElement, key)
    })

    this.destroyRef.onDestroy(() => {
      this.removeWidget()
    })
  }

  public reset(): void {
    const api = this.getTurnstileApi()
    if (api && this.widgetId) {
      api.reset(this.widgetId)
    }
    this.tokenChange.emit(null)
  }

  private getTurnstileApi(): TurnstileApi | undefined {
    const win = this.document.defaultView as WindowWithTurnstile | null
    return win?.turnstile
  }

  private ensureScriptAndRender(element: HTMLElement, sitekey: string): void {
    const existingApi = this.getTurnstileApi()
    if (existingApi) {
      this.renderWidget(existingApi, element, sitekey)
      return
    }

    let script = this.document.getElementById(TURNSTILE_SCRIPT_ID) as HTMLScriptElement | null
    if (!script) {
      script = this.document.createElement('script')
      script.id = TURNSTILE_SCRIPT_ID
      script.src = TURNSTILE_SCRIPT_SRC
      script.async = true
      script.defer = true
      this.document.head.appendChild(script)
    }

    const onLoad = () => {
      const api = this.getTurnstileApi()
      if (api) {
        this.renderWidget(api, element, sitekey)
      }
    }
    script.addEventListener('load', onLoad, { once: true })
  }

  private renderWidget(api: TurnstileApi, element: HTMLElement, sitekey: string): void {
    this.removeWidget()
    this.widgetId = api.render(element, {
      sitekey,
      'theme': 'auto',
      'callback': (token: string) => this.tokenChange.emit(token),
      'expired-callback': () => this.tokenChange.emit(null),
      'error-callback': () => this.tokenChange.emit(null),
    })
  }

  private removeWidget(): void {
    const api = this.getTurnstileApi()
    if (api && this.widgetId) {
      api.remove(this.widgetId)
      this.widgetId = null
    }
  }
}
