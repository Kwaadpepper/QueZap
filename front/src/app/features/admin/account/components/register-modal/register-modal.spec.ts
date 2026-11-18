import { ErrorHandler } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideNoopAnimations } from '@angular/platform-browser/animations'

import { MessageService } from 'primeng/api'
import { of, throwError } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'

import { REGISTER_SERVICE, RegisterService } from '../../services'

import { RegisterModal } from './register-modal'

describe('RegisterModal', () => {
  let component: RegisterModal
  let fixture: ComponentFixture<RegisterModal>

  const registerServiceMock: jest.Mocked<RegisterService> = { register: jest.fn() }
  const messageServiceMock = { add: jest.fn() }
  const errorHandlerMock = { handleError: jest.fn() }

  beforeEach(async () => {
    jest.clearAllMocks()
    registerServiceMock.register.mockReturnValue(of({ kind: 'success', result: undefined }))

    await TestBed.configureTestingModule({
      imports: [RegisterModal],
      providers: [
        provideNoopAnimations(),
        { provide: REGISTER_SERVICE, useValue: registerServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
        { provide: ErrorHandler, useValue: errorHandlerMock },
      ],
    })
      .compileComponents()

    fixture = TestBed.createComponent(RegisterModal)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should bind email input via [formField] and enable submit on valid email', () => {
    component.visible.set(true)
    fixture.detectChanges()

    const input = document.querySelector('input#email') as HTMLInputElement | null
    expect(input).not.toBeNull()

    input!.value = 'teacher@quezap.com'
    input!.dispatchEvent(new Event('input', { bubbles: true }))
    fixture.detectChanges()

    expect(component['registerForm'].email().value()).toBe('teacher@quezap.com')
    expect(component['registerForm']().valid()).toBe(true)
  })

  it('should call registerService, show toast, and close modal on success', async () => {
    component.visible.set(true)
    fixture.detectChanges()
    component['userInfo'].set({ email: 'teacher@quezap.com' })
    fixture.detectChanges()

    component['onRegister']()
    await fixture.whenStable()

    expect(registerServiceMock.register).toHaveBeenCalledWith('teacher@quezap.com')
    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    expect(component.visible()).toBe(false)
  })

  it('should attach validation errors to the form on ValidationError failure', async () => {
    registerServiceMock.register.mockReturnValueOnce(
      of({ kind: 'failure', error: new ValidationError({ email: ['Email déjà utilisé'] }) }),
    )
    component['userInfo'].set({ email: 'teacher@quezap.com' })
    fixture.detectChanges()

    component['onRegister']()
    await fixture.whenStable()

    expect(component['registerForm'].email().errors()).toHaveLength(1)
  })

  it('should display error toast and call errorHandler on unexpected error', async () => {
    registerServiceMock.register.mockReturnValueOnce(throwError(() => new Error('Network failure')))
    component['userInfo'].set({ email: 'teacher@quezap.com' })
    fixture.detectChanges()

    component['onRegister']()
    await fixture.whenStable()

    expect(messageServiceMock.add).toHaveBeenCalledWith(expect.objectContaining({ severity: 'error' }))
    expect(errorHandlerMock.handleError).toHaveBeenCalledTimes(1)
  })
})
