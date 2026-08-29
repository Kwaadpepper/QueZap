import { TestBed } from '@angular/core/testing'

import { AuthTokens } from '@quezap/domain/models'

import { TokenPersitance } from './token-persitance'

describe('TokenPersitance', () => {
  let service: TokenPersitance

  const mockTokens: AuthTokens = {
    accessToken: 'access-123',
    refreshToken: 'refresh-456',
    tokenType: 'Bearer',
  }

  beforeEach(() => {
    localStorage.clear()
    TestBed.configureTestingModule({})
    service = TestBed.inject(TokenPersitance)
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })

  it('should save and retrieve tokens from localStorage', () => {
    service.saveTokens(mockTokens)
    const retrieved = service.getTokens()

    expect(retrieved).toEqual(mockTokens)
  })

  it('should remove tokens from localStorage', () => {
    service.saveTokens(mockTokens)
    expect(service.getTokens()).toEqual(mockTokens)

    service.removeTokens()
    expect(service.getTokens()).toBeUndefined()
  })

  it('should return undefined when no tokens are stored', () => {
    expect(service.getTokens()).toBeUndefined()
  })

  it('should handle corrupted JSON in localStorage gracefully', () => {
    localStorage.setItem('auth_tokens', 'invalid{json')
    expect(service.getTokens()).toBeUndefined()
  })

  it('should fall back to memory cache when localStorage throws error', () => {
    const origSetItem = localStorage.setItem
    const origGetItem = localStorage.getItem
    localStorage.setItem = jest.fn(() => {
      throw new Error('QuotaExceeded')
    })
    localStorage.getItem = jest.fn(() => {
      throw new Error('SecurityBlocked')
    })

    service.saveTokens(mockTokens)
    const retrieved = service.getTokens()

    expect(retrieved).toEqual(mockTokens)

    service.removeTokens()
    expect(service.getTokens()).toBeUndefined()

    localStorage.setItem = origSetItem
    localStorage.getItem = origGetItem
  })
})
