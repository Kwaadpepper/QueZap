import { computed, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, UrlTree } from '@angular/router'

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
            parseUrl: jest.fn<UrlTree, [string]>(() => new UrlTree()),
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

    it('should return UrlTree when user is not logged in', () => {
      isLoggedInSignal.set(false)

      const result = guard.canActivate()

      expect(result).toBeInstanceOf(UrlTree)
    })
  })

  describe('canActivateChild', () => {
    it('should return true when user is logged in', () => {
      isLoggedInSignal.set(true)

      const result = guard.canActivateChild()

      expect(result).toBe(true)
    })

    it('should return UrlTree when user is not logged in', () => {
      isLoggedInSignal.set(false)

      const result = guard.canActivateChild()

      expect(result).toBeInstanceOf(UrlTree)
    })
  })
})
