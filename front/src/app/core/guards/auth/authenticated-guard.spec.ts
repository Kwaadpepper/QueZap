import { computed, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { AuthenticatedUserStore } from '@quezap/shared/stores'

import { AuthenticatedGuard } from './authenticated-guard'

describe('AuthenticatedGuard', () => {
  let guard: AuthenticatedGuard
  const isLoggedInSignal = signal(false)

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        AuthenticatedGuard,
        {
          provide: AuthenticatedUserStore,
          useValue: {
            isLoggedIn: computed(() => isLoggedInSignal()),
          },
        },
        {
          provide: Router,
          useValue: {
            parseUrl: (url: string) => url !== '/auth/login',
          },
        },
      ],
    }).compileComponents()

    guard = TestBed.inject(AuthenticatedGuard)
  })

  it('should be created', () => {
    expect(guard).toBeTruthy()
  })

  describe('canActivate', () => {
    it('should return true when user is logged in', () => {
      isLoggedInSignal.set(true)

      const result = guard.canActivate()

      expect(result).toBe(true)
    })

    it('should return false when user is not logged in', () => {
      isLoggedInSignal.set(false)

      const result = guard.canActivate()

      expect(result).toBe(false)
    })
  })

  describe('canActivateChild', () => {
    it('should return true when user is logged in', () => {
      isLoggedInSignal.set(true)

      const result = guard.canActivateChild()

      expect(result).toBe(true)
    })

    it('should return false when user is not logged in', () => {
      isLoggedInSignal.set(false)

      const result = guard.canActivateChild()

      expect(result).toBe(false)
    })
  })
})
