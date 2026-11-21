import { TestBed } from '@angular/core/testing'

import { Config } from './config'

describe('ConfigService', () => {
  let service: Config
  const originalFetch = globalThis.fetch

  beforeEach(() => {
    TestBed.configureTestingModule({})
    service = TestBed.inject(Config)
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
    jest.restoreAllMocks()
  })

  it('should be created with default environment values', () => {
    expect(service).toBeTruthy()
    expect(service.appConfig.value().appName).toBe('QueZap')
    expect(service.appConfig.value().apiUrl).toBe('/apiv1')
  })

  it('should override values from /config.json when loaded', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => 'application/json; charset=utf-8' },
      json: async () => ({
        appName: 'Quiz Classe',
        authorName: 'Jérémy',
        authorEmail: 'jeremy@quezap.fr',
        apiUrl: 'https://quiz.example.com/apiv1',
      }),
    } as unknown as Response)

    const loaded = await service.load()

    expect(globalThis.fetch).toHaveBeenCalledWith('/config.json', { cache: 'no-store' })
    expect(loaded.appName).toBe('Quiz Classe')
    expect(loaded.authorName).toBe('Jérémy')
    expect(loaded.authorEmail).toBe('jeremy@quezap.fr')
    expect(loaded.apiUrl).toBe('https://quiz.example.com/apiv1')
    expect(service.appConfig.value()).toEqual(loaded)
  })

  it('should keep valid overrides and ignore invalid ones with a warning', async () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined)
    const defaultEmail = service.appConfig.value().authorEmail

    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => 'application/json' },
      json: async () => ({
        authorName: 'Custom Teacher',
        authorEmail: 'not-a-valid-email',
      }),
    } as unknown as Response)

    const loaded = await service.load()

    expect(loaded.authorName).toBe('Custom Teacher')
    expect(loaded.authorEmail).toBe(defaultEmail)
    expect(warnSpy).toHaveBeenCalled()
  })

  it('should ignore non-JSON fallback responses gracefully', async () => {
    const initial = service.appConfig.value()

    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => 'text/html' },
      json: async () => {
        throw new Error('Should not parse HTML as JSON')
      },
    } as unknown as Response)

    const loaded = await service.load()
    expect(loaded).toEqual(initial)
  })

  it('should keep defaults when /config.json is unavailable', async () => {
    const initial = service.appConfig.value()

    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: false,
      headers: { get: () => 'text/plain' },
    } as unknown as Response)

    const loaded = await service.load()
    expect(loaded).toEqual(initial)
  })
})
