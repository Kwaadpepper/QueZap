import { ErrorHandler } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { MessageService } from 'primeng/api'
import { of, throwError } from 'rxjs'

import { AuthenticatedUserStore } from '@quezap/shared/stores/authenticated-user'

import { LogoutButton } from './logout-button'

describe('LogoutButton', () => {
  let component: LogoutButton
  let fixture: ComponentFixture<LogoutButton>

  const routerMock = { navigate: jest.fn().mockResolvedValue(true) }
  const messageServiceMock = { add: jest.fn() }
  const errorHandlerMock = { handleError: jest.fn() }
  const authStoreMock = { logout: jest.fn() }

  beforeEach(async () => {
    jest.clearAllMocks()
    authStoreMock.logout.mockReturnValue(of(void 0))

    await TestBed.configureTestingModule({
      imports: [LogoutButton],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: MessageService, useValue: messageServiceMock },
        { provide: ErrorHandler, useValue: errorHandlerMock },
        { provide: AuthenticatedUserStore, useValue: authStoreMock },
      ],
    })
      .compileComponents()

    fixture = TestBed.createComponent(LogoutButton)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should logout, redirect to /auth/login, and show notification', async () => {
    component['onLogout']()
    await fixture.whenStable()

    expect(authStoreMock.logout).toHaveBeenCalledTimes(1)
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login'])
    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'info' }))
    expect(component['running']()).toBe(false)
  })

  it('should handle logout errors and still navigate to /auth/login', async () => {
    authStoreMock.logout.mockReturnValueOnce(throwError(() => new Error('Logout error')))

    component['onLogout']()
    await fixture.whenStable()

    expect(errorHandlerMock.handleError).toHaveBeenCalledTimes(1)
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login'])
    expect(component['running']()).toBe(false)
  })
})
