import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { Confirmation, ConfirmationService } from 'primeng/api'

import { ActiveSessionStore } from '../../stores'

import { ExitButton } from './exit-button'

describe('ExitButton', () => {
  let component: ExitButton
  let fixture: ComponentFixture<ExitButton>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let router: Router

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExitButton],
      providers: [ActiveSessionStore],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    router = TestBed.inject(Router)

    fixture = TestBed.createComponent(ExitButton)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call sessionStore.leaveSession() and navigate to / when confirming quit', () => {
    const leaveSpy = jest.spyOn(sessionStore, 'leaveSession')
    const navSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true)
    const confirmationService = fixture.debugElement.injector.get(ConfirmationService)
    jest.spyOn(confirmationService, 'confirm').mockImplementation((confirmation: Confirmation) => {
      confirmation.accept?.()
      return confirmationService
    })

    component['onQuitQuizz']({ currentTarget: document.createElement('button') } as unknown as Event)

    expect(leaveSpy).toHaveBeenCalled()
    expect(navSpy).toHaveBeenCalledWith('/')
  })
})
