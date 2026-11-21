import { signal } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'

import { Config, Environment } from '@quezap/core/services/config/config'

import { TurnstileWidget } from './turnstile'

describe('TurnstileWidget', () => {
  const baseConfig = {
    env: Environment.PROD,
    useMockData: false,
    appName: 'QueZap',
    authorName: 'Author',
    authorEmail: 'author@example.net',
    apiUrl: '/apiv1',
  }

  afterEach(() => {
    delete (globalThis as unknown as { turnstile?: unknown }).turnstile
    document.getElementById('cf-turnstile-script')?.remove()
  })

  it('should stay disabled and render nothing when turnstileSiteKey is absent', async () => {
    const mockConfig = {
      appConfig: { value: signal(baseConfig) },
      debug: signal(false),
    }

    await TestBed.configureTestingModule({
      imports: [TurnstileWidget],
      providers: [{ provide: Config, useValue: mockConfig }],
    }).compileComponents()

    const fixture: ComponentFixture<TurnstileWidget> = TestBed.createComponent(TurnstileWidget)
    fixture.detectChanges()
    await fixture.whenStable()

    expect(fixture.componentInstance.enabled()).toBe(false)
    expect(fixture.nativeElement.querySelector('div')).toBeNull()
  })

  it('should render widget and emit token when turnstile API is available', async () => {
    let capturedCallback: ((token: string) => void) | undefined
    const renderMock = jest.fn((_el: HTMLElement, opts: { callback?: (t: string) => void }) => {
      capturedCallback = opts.callback
      return 'widget-1'
    })
    const resetMock = jest.fn()
    const removeMock = jest.fn()

    ;(globalThis as unknown as { turnstile?: unknown }).turnstile = {
      render: renderMock,
      reset: resetMock,
      remove: removeMock,
    }

    const mockConfig = {
      appConfig: { value: signal({ ...baseConfig, turnstileSiteKey: 'site-key-123' }) },
      debug: signal(false),
    }

    await TestBed.configureTestingModule({
      imports: [TurnstileWidget],
      providers: [{ provide: Config, useValue: mockConfig }],
    }).compileComponents()

    const fixture = TestBed.createComponent(TurnstileWidget)
    const emitted: (string | null)[] = []
    fixture.componentInstance.tokenChange.subscribe(t => emitted.push(t))

    fixture.detectChanges()
    await fixture.whenStable()

    expect(fixture.componentInstance.enabled()).toBe(true)
    expect(renderMock).toHaveBeenCalledTimes(1)

    capturedCallback?.('cf-token-xyz')
    expect(emitted).toEqual(['cf-token-xyz'])

    fixture.componentInstance.reset()
    expect(resetMock).toHaveBeenCalledWith('widget-1')
    expect(emitted).toEqual(['cf-token-xyz', null])

    fixture.destroy()
    expect(removeMock).toHaveBeenCalledWith('widget-1')
  })
})
