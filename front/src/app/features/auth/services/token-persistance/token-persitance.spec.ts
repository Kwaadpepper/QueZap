import { TestBed } from '@angular/core/testing'

import { AuthTokens } from '@quezap/domain/models'
import { JWT } from '@quezap/domain/types'

import { TokenPersitance } from './token-persitance'

describe('TokenPersitance', () => {
  let service: TokenPersitance

  const mockTokens: AuthTokens = { accessToken: 'access-123' as JWT }

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

  it('should save and retrieve tokens strictly in memory without touching localStorage', () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem')
    const getItemSpy = jest.spyOn(Storage.prototype, 'getItem')

    service.saveTokens(mockTokens)
    const retrieved = service.getTokens()

    expect(retrieved).toEqual(mockTokens)
    expect(localStorage.getItem('auth_tokens')).toBeNull()
    expect(setItemSpy).not.toHaveBeenCalled()
    expect(getItemSpy).toHaveBeenCalledTimes(1)

    setItemSpy.mockRestore()
    getItemSpy.mockRestore()
  })

  it('should remove tokens from memory', () => {
    service.saveTokens(mockTokens)
    expect(service.getTokens()).toEqual(mockTokens)

    service.removeTokens()
    expect(service.getTokens()).toBeUndefined()
  })

  it('should return undefined when no tokens are stored', () => {
    expect(service.getTokens()).toBeUndefined()
  })
})
