import { TestBed } from '@angular/core/testing'

import * as sdk from '@quezap/core/api-client/sdk.gen'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import { SessionObserverSseService } from './session-observer-sse'

class MockEventSource {
  static readonly CONNECTING = 0
  static readonly OPEN = 1
  static readonly CLOSED = 2
  static instances: MockEventSource[] = []

  public readyState = MockEventSource.OPEN
  public onopen: ((ev: Event) => void) | null = null
  public onmessage: ((ev: MessageEvent) => void) | null = null
  public onerror: ((ev: Event) => void) | null = null
  private listeners = new Map<string, ((ev: MessageEvent) => void)[]>()

  constructor(public readonly url: string) {
    MockEventSource.instances.push(this)
  }

  addEventListener(name: string, listener: (ev: MessageEvent) => void): void {
    const existing = this.listeners.get(name) ?? []
    this.listeners.set(name, [...existing, listener])
  }

  emitNamedEvent(name: string, data: unknown, lastEventId = ''): void {
    const ev = { data: JSON.stringify(data), lastEventId } as MessageEvent
    for (const listener of this.listeners.get(name) ?? []) {
      listener(ev)
    }
  }

  triggerClosedError(): void {
    this.readyState = MockEventSource.CLOSED
    this.onerror?.(new Event('error'))
  }

  close = jest.fn(() => {
    this.readyState = MockEventSource.CLOSED
  })
}

describe('SessionObserverSseService', () => {
  let service: SessionObserverSseService
  const originalEventSource = globalThis.EventSource

  beforeEach(() => {
    jest.useFakeTimers()
    MockEventSource.instances = []
    Object.defineProperty(globalThis, 'EventSource', {
      value: MockEventSource,
      writable: true,
      configurable: true,
    })
    localStorage.clear()
    sessionStorage.clear()

    TestBed.configureTestingModule({ providers: [SessionObserverSseService, ActiveSessionPersistence] })
    service = TestBed.inject(SessionObserverSseService)
  })

  afterEach(() => {
    service.disconnect()
    jest.useRealTimers()
    jest.restoreAllMocks()
    Object.defineProperty(globalThis, 'EventSource', {
      value: originalEventSource,
      writable: true,
      configurable: true,
    })
  })

  it('should reconnect with exponential backoff and lastEventId when EventSource closes', async () => {
    const sessionId = '018e3a2b-8888-7def-8123-000000000001'
    const getByIdSpy = jest.spyOn(sdk, 'getById2').mockResolvedValue({
      data: { id: sessionId, status: 'STARTED' },
      error: undefined,
      request: {} as never,
      response: { status: 200 } as never,
    })

    service.connect(sessionId)
    expect(MockEventSource.instances).toHaveLength(1)

    // Receive event with id=42 and points=0 (verify ?? 1000 does not overwrite 0)
    MockEventSource.instances[0].emitNamedEvent('QUESTION_START', {
      type: 'QUESTION_START',
      sessionNumber: 123,
      sessionId,
      timestamp: new Date().toISOString(),
      payload: {
        questionId: '018e3a2b-8888-7def-8123-000000000010',
        label: 'Sondage',
        type: 'QUIZZ',
        points: 0,
        timerSeconds: 20,
        answers: [{ index: 0, label: 'Oui' }, { index: 1, label: 'Non' }],
      },
    }, '42')

    const q = service.question()
    expect(q.type).toBe('QUIZZ')
    if (q.type === 'QUIZZ') {
      expect(q.answers[0].points).toBe(0)
    }

    // Simulate prolonged drop -> EventSource.CLOSED
    MockEventSource.instances[0].triggerClosedError()
    expect(MockEventSource.instances).toHaveLength(1)

    // Advance 1s backoff -> checks session via getById2 and reopens EventSource with lastEventId=42
    jest.advanceTimersByTime(1000)
    await Promise.resolve()
    await Promise.resolve()

    expect(getByIdSpy).toHaveBeenCalledWith(expect.objectContaining({ path: { id: sessionId } }))
    expect(MockEventSource.instances).toHaveLength(2)
    expect(MockEventSource.instances[1].url).toContain('lastEventId=42')
  })

  it('should stop reconnecting when server returns 404 or 403 or ENDED', async () => {
    const sessionId = '018e3a2b-8888-7def-8123-000000000002'
    jest.spyOn(sdk, 'getById2').mockResolvedValue({
      data: undefined,
      error: { message: 'Not found' },
      request: {} as never,
      response: { status: 404 } as never,
    })

    service.connect(sessionId)
    MockEventSource.instances[0].triggerClosedError()

    jest.advanceTimersByTime(1000)
    await Promise.resolve()
    await Promise.resolve()

    // Should NOT create a second EventSource and should NOT schedule more timers
    expect(MockEventSource.instances).toHaveLength(1)
    jest.advanceTimersByTime(60000)
    await Promise.resolve()
    expect(MockEventSource.instances).toHaveLength(1)
  })

  it('should not reconnect after SESSION_ENDED is received', () => {
    const sessionId = '018e3a2b-8888-7def-8123-000000000003'
    service.connect(sessionId)

    const source = MockEventSource.instances[0]
    source.emitNamedEvent('SESSION_ENDED', {
      type: 'SESSION_ENDED',
      sessionNumber: 123,
      sessionId,
      timestamp: new Date().toISOString(),
      payload: { finalLeaderboard: [{ rank: 1, participantName: 'Léa', score: 1000, correctAnswers: 1 }] },
    }, '99')

    expect(source.close).toHaveBeenCalled()
    source.triggerClosedError()
    jest.advanceTimersByTime(5000)
    expect(MockEventSource.instances).toHaveLength(1)
  })
})
