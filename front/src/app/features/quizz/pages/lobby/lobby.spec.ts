import { ComponentFixture, TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'

import { patchState, signalState } from '@ngrx/signals'

import { ActiveSessionStore } from '../../stores'

import { Lobby } from './lobby'

describe('Lobby', () => {
  let component: Lobby
  let fixture: ComponentFixture<Lobby>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let router: Router

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Lobby],
      providers: [ActiveSessionStore],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    router = TestBed.inject(Router)

    fixture = TestBed.createComponent(Lobby)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should wait for nickname before navigating to /quizz when session is already running', () => {
    const navSpy = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true)

    patchState(sessionStore, { _sessionIsRunning: true })
    fixture.detectChanges()

    expect(navSpy).not.toHaveBeenCalled()

    patchState(sessionStore, { _nickname: signalState({ value: 'Alice', remembered: false }) })
    fixture.detectChanges()

    expect(navSpy).toHaveBeenCalledWith('/quizz', { skipLocationChange: true })
  })
})
