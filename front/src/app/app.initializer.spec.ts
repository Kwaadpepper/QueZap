import { DOCUMENT, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'

import { of, throwError } from 'rxjs'

import { AppInitializer } from './app.initializer'
import { client } from './core/api-client/client.gen'
import { Config } from './core/services/config/config'
import { AuthenticatedUserStore } from './shared/stores/authenticated-user'

jest.mock('./zod.initializer', () => ({ initZod: jest.fn(() => of(undefined)) }))

describe('AppInitializer', () => {
  const createSplashScreen = (doc: Document): HTMLElement => {
    const existing = doc.getElementById('app-loading-screen')
    if (existing) {
      return existing
    }
    const el = doc.createElement('div')
    el.id = 'app-loading-screen'
    doc.body.appendChild(el)
    return el
  }

  afterEach(() => {
    jest.useRealTimers()
    jest.restoreAllMocks()
    const el = document.getElementById('app-loading-screen')
    el?.remove()
  })

  it('should configure client baseUrl, load initial state, and hide splash screen', async () => {
    jest.useFakeTimers()
    const setConfigSpy = jest.spyOn(client, 'setConfig')
    const mockAuthStore = { loadInitialState: jest.fn(() => of(void 0)) }
    const mockConfig = {
      appConfig: { value: () => ({ apiUrl: '/apiv1' }) },
      debug: signal(false),
    }

    TestBed.configureTestingModule({
      providers: [
        { provide: Config, useValue: mockConfig },
        { provide: AuthenticatedUserStore, useValue: mockAuthStore },
      ],
    })

    const doc = TestBed.inject(DOCUMENT)
    const splash = createSplashScreen(doc)

    await TestBed.runInInjectionContext(() => AppInitializer())

    expect(setConfigSpy).toHaveBeenCalledWith({ baseUrl: globalThis.location.origin })
    expect(mockAuthStore.loadInitialState).toHaveBeenCalledTimes(1)
    expect(splash.style.opacity).toBe('0')

    jest.advanceTimersByTime(300)
    expect(doc.getElementById('app-loading-screen')).toBeNull()
  })

  it('should boot silently without window.confirm when auth initialization fails', async () => {
    const confirmSpy = jest.spyOn(globalThis, 'confirm')
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined)
    const mockAuthStore = { loadInitialState: jest.fn(() => throwError(() => new Error('Expired token'))) }
    const mockConfig = {
      appConfig: { value: () => ({ apiUrl: 'https://api.example.com/apiv1' }) },
      debug: signal(true),
    }
    const setConfigSpy = jest.spyOn(client, 'setConfig')

    TestBed.configureTestingModule({
      providers: [
        { provide: Config, useValue: mockConfig },
        { provide: AuthenticatedUserStore, useValue: mockAuthStore },
      ],
    })

    const doc = TestBed.inject(DOCUMENT)
    const splash = createSplashScreen(doc)

    await expect(TestBed.runInInjectionContext(() => AppInitializer())).resolves.toBeDefined()

    expect(setConfigSpy).toHaveBeenCalledWith({ baseUrl: 'https://api.example.com' })
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(warnSpy).toHaveBeenCalled()
    expect(splash.style.opacity).toBe('0')
  })
})
