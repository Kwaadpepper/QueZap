import { TestBed } from '@angular/core/testing'

import { SessionCode } from '@quezap/domain/models'

import { ActiveSessionPersistence } from './active-session-persistence'

describe('ActiveSessionPersistence', () => {
  let service: ActiveSessionPersistence

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    TestBed.configureTestingModule({})
    service = TestBed.inject(ActiveSessionPersistence)
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })

  it('should persist and retrieve session data', () => {
    service.persist({ code: 'ABCD' as SessionCode, nickname: 'Alice' })
    const data = service.retrieve()

    expect(data).toEqual({
      code: 'ABCD',
      nickname: 'Alice',
    })
  })

  it('should patch existing session data', () => {
    service.persist({ code: 'ABCD' as SessionCode, nickname: 'Alice' })
    service.patch({ nickname: 'Bob' })

    const data = service.retrieve()
    expect(data?.code).toBe('ABCD')
    expect(data?.nickname).toBe('Bob')
  })

  it('should persist and update nickname', () => {
    service.persistNickname('Charlie')
    expect(service.retrieve()?.nickname).toBe('Charlie')

    service.persistNickname('David')
    expect(service.retrieve()?.nickname).toBe('David')
  })

  it('should save and retrieve participation data and tokens', () => {
    service.saveParticipationData('session-123', 'token-xyz', 'Player1')

    expect(service.getSessionId()).toBe('session-123')
    expect(service.getParticipationToken()).toBe('token-xyz')
    expect(service.getParticipantName()).toBe('Player1')

    const data = service.retrieve()
    expect(data?.sessionId).toBe('session-123')
    expect(data?.participationToken).toBe('token-xyz')
    expect(data?.nickname).toBe('Player1')
  })

  it('should clear all stored session keys', () => {
    service.saveParticipationData('session-123', 'token-xyz', 'Player1')
    service.clear()

    expect(service.retrieve()).toBeNull()
    expect(service.getSessionId()).toBeNull()
    expect(service.getParticipationToken()).toBeNull()
    expect(service.getParticipantName()).toBeNull()
  })

  it('should clear session while keeping nickname if present', () => {
    service.persist({ code: 'ABCD' as SessionCode, nickname: 'PreservedUser' })
    service.clearKeepingNickname()

    const retrieved = service.retrieve()
    expect(retrieved?.code).toBe('')
    expect(retrieved?.nickname).toBe('PreservedUser')
  })

  it('should clear completely if nickname is absent when clearKeepingNickname is called', () => {
    service.persist({ code: 'ABCD' as SessionCode })
    service.clearKeepingNickname()

    expect(service.retrieve()).toBeNull()
  })

  it('should handle corrupted JSON in localStorage gracefully', () => {
    localStorage.setItem('activeSessionCode', 'invalid-json{')
    expect(service.retrieve()).toBeNull()
  })

  it('should fall back to memory cache when storage throws an exception', () => {
    const origSetItem = localStorage.setItem
    const origGetItem = localStorage.getItem
    localStorage.setItem = jest.fn(() => {
      throw new Error('QuotaExceededError')
    })
    localStorage.getItem = jest.fn(() => {
      throw new Error('SecurityError')
    })

    service.persist({ code: 'MEM1' as SessionCode, nickname: 'MemoryUser' })
    const retrieved = service.retrieve()

    expect(retrieved).not.toBeNull()
    expect(retrieved?.nickname).toBe('MemoryUser')

    localStorage.setItem = origSetItem
    localStorage.getItem = origGetItem
  })
})
