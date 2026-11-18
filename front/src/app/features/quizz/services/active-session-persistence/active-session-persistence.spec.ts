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
    jest.restoreAllMocks()
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

  it('should save and retrieve participation data, tokens and participantName in localStorage', () => {
    service.saveParticipationData('session-123', 'token-xyz', 'Player1')

    expect(service.getSessionId()).toBe('session-123')
    expect(service.getParticipationToken()).toBe('token-xyz')
    expect(service.getParticipantName()).toBe('Player1')
    expect(localStorage.getItem('quezap_participant_name:session-123')).toContain('Player1')

    // Simulate closing and reopening the tab (clearing sessionStorage only)
    sessionStorage.clear()
    expect(service.getParticipantName('session-123')).toBe('Player1')

    const data = service.retrieve()
    expect(data?.sessionId).toBe('session-123')
    expect(data?.participationToken).toBe('token-xyz')
    expect(data?.nickname).toBe('Player1')
  })

  it('should clear all stored session keys', () => {
    service.saveParticipationData('session-123', 'token-xyz', 'Player1')
    service.saveSubmittedChoice('q-1', { answerIndex: 1, label: 'B' }, 'session-123')
    service.saveFinalResults({
      rank: 1,
      totalParticipants: 5,
      score: 1000,
      correctAnswers: 1,
      isPodium: true,
    }, 'session-123')
    service.clear()

    expect(service.retrieve()).toBeNull()
    expect(service.getSessionId()).toBeNull()
    expect(service.getParticipationToken()).toBeNull()
    expect(service.getParticipantName()).toBeNull()
    expect(service.getSubmittedChoice('q-1', 'session-123')).toBeUndefined()
    expect(service.getFinalResults('session-123')).toBeNull()
    expect(localStorage.getItem('quezap_submitted_choice:session-123')).toBeNull()
    expect(localStorage.getItem('quezap_final_results:session-123')).toBeNull()
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

  it('should persist and retrieve submitted choice in localStorage scoped by sessionId', () => {
    service.setSessionId('session-A')
    service.saveSubmittedChoice('q-1', { answerIndex: 2, label: 'Tulipe' })

    expect(localStorage.getItem('quezap_submitted_choice:session-A')).toContain('Tulipe')
    expect(sessionStorage.getItem('quezap_submitted_choice')).toBeNull()
    expect(service.getSubmittedChoice('q-1')).toEqual({ answerIndex: 2, label: 'Tulipe' })
    expect(service.getSubmittedChoice('q-2')).toBeUndefined()
    expect(service.getSubmittedChoice('q-1', 'session-B')).toBeUndefined()

    // Switching sessionId cleans up old session's scoped keys
    service.setSessionId('session-B')
    expect(localStorage.getItem('quezap_submitted_choice:session-A')).toBeNull()
    expect(service.getSubmittedChoice('q-1', 'session-A')).toBeUndefined()

    service.clear()
    expect(service.getSubmittedChoice('q-1')).toBeUndefined()
  })

  it('should persist finalResults in localStorage scoped by sessionId across tab close', () => {
    service.setSessionId('session-99')
    service.saveFinalResults({
      rank: 2,
      totalParticipants: 10,
      score: 1850,
      correctAnswers: 4,
      isPodium: true,
    })

    expect(localStorage.getItem('quezap_final_results:session-99')).toContain('1850')
    expect(sessionStorage.getItem('quezap_final_results')).toBeNull()

    // Simulate reopening tab in a fresh service instance
    sessionStorage.clear()
    TestBed.resetTestingModule()
    TestBed.configureTestingModule({})
    const freshService = TestBed.inject(ActiveSessionPersistence)

    expect(freshService.getFinalResults('session-99')).toEqual({
      rank: 2,
      totalParticipants: 10,
      score: 1850,
      correctAnswers: 4,
      isPodium: true,
    })
    expect(freshService.getFinalResults('other-session')).toBeNull()
  })

  it('should purge expired scoped entries on read and retrieve', () => {
    const expiredPayload = JSON.stringify({
      sessionId: 'session-old',
      questionId: 'q-1',
      answerIndex: 0,
      label: 'Expired',
      expiresAt: Date.now() - 1000,
    })
    localStorage.setItem('quezap_submitted_choice:session-old', expiredPayload)

    expect(service.getSubmittedChoice('q-1', 'session-old')).toBeUndefined()
    expect(localStorage.getItem('quezap_submitted_choice:session-old')).toBeNull()

    localStorage.setItem('quezap_final_results:session-old', JSON.stringify({
      sessionId: 'session-old',
      results: { rank: 1, totalParticipants: 1, score: 100, isPodium: true },
      expiresAt: Date.now() - 1000,
    }))
    service.cleanupExpiredEntries()
    expect(localStorage.getItem('quezap_final_results:session-old')).toBeNull()
  })
})
