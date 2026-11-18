import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { patchState, signalState } from '@ngrx/signals'

import { SessionCode, SessionId } from '@quezap/domain/models'

import { ActiveSessionStore } from '../../stores'

import { Join } from './join'

describe('Join', () => {
  let component: Join
  let fixture: ComponentFixture<Join>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let router: Router

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Join],
      providers: [ActiveSessionStore],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    router = TestBed.inject(Router)
    fixture = TestBed.createComponent(Join)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should auto-resume to /quizz when active running session and nickname are restored without route code', () => {
    const navSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true)

    patchState(sessionStore, {
      _sessionIsLoaded: true,
      _session: {
        id: '018e3a2b-8888-7def-8123-000000000001' as SessionId,
        code: '20082' as SessionCode,
        name: 'Session active',
        status: 'STARTED',
        startedAt: new Date(),
        endedAt: null,
      },
      _nickname: signalState({ value: 'Léa', remembered: false }),
    })
    fixture.detectChanges()

    expect(navSpy).toHaveBeenCalledWith(['/quizz'], { skipLocationChange: true })
  })

  it('should not auto-resume when restored session has ended', () => {
    const navSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true)

    patchState(sessionStore, {
      _sessionIsLoaded: true,
      _session: {
        id: '018e3a2b-8888-7def-8123-000000000001' as SessionId,
        code: '20082' as SessionCode,
        name: 'Session terminée',
        status: 'ENDED',
        startedAt: new Date(),
        endedAt: new Date(),
      },
      _nickname: signalState({ value: 'Léa', remembered: false }),
    })
    fixture.detectChanges()

    expect(navSpy).not.toHaveBeenCalled()
  })
})
