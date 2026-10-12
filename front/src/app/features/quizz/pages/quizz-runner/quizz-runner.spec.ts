import { ComponentFixture, TestBed } from '@angular/core/testing'

import { patchState } from '@ngrx/signals'

import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'
import { ActiveSessionStore, TimerStore } from '../../stores'

import { QuizzRunner } from './quizz-runner'

describe('QuizzRunner', () => {
  let component: QuizzRunner
  let fixture: ComponentFixture<QuizzRunner>
  let sessionStore: InstanceType<typeof ActiveSessionStore>
  let timerStore: InstanceType<typeof TimerStore>
  let persistence: ActiveSessionPersistence

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuizzRunner],
      providers: [ActiveSessionStore, TimerStore, ActiveSessionPersistence],
    }).compileComponents()

    sessionStore = TestBed.inject(ActiveSessionStore)
    timerStore = TestBed.inject(TimerStore)
    persistence = TestBed.inject(ActiveSessionPersistence)

    fixture = TestBed.createComponent(QuizzRunner)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should return 0 points earned when myRanking is undefined', () => {
    expect(component['pointsEarned']()).toBe(0)
  })

  it('should compute points earned from rank.delta when delta is present', () => {
    jest.spyOn(persistence, 'getParticipantName').mockReturnValue('Alice')
    patchState(sessionStore, {
      lastLeaderboard: {
        slideIndex: 1,
        leaderboard: [
          {
            rank: 1,
            participantName: 'Alice',
            score: 950,
            delta: 950,
            previousScore: 0,
            rankDelta: 0,
          },
        ],
      },
    })
    fixture.detectChanges()

    expect(component['pointsEarned']()).toBe(950)
  })

  it('should fallback to score - previousScore when delta is undefined', () => {
    jest.spyOn(persistence, 'getParticipantName').mockReturnValue('Bob')
    patchState(sessionStore, {
      lastLeaderboard: {
        slideIndex: 2,
        leaderboard: [
          {
            rank: 2,
            participantName: 'Bob',
            score: 1500,
            previousScore: 800,
            rankDelta: 0,
          },
        ],
      },
    })
    fixture.detectChanges()

    expect(component['pointsEarned']()).toBe(700)
  })

  it('should clear timer when lastReveal is set', () => {
    const clearTimerSpy = jest.spyOn(timerStore, 'clearTimer')
    patchState(sessionStore, {
      lastReveal: {
        slideIndex: 1,
        correctIndices: [0],
        stats: [],
      },
    })
    fixture.detectChanges()

    expect(clearTimerSpy).toHaveBeenCalled()
  })

  it('should display pause alert banner when isSessionPaused is true', () => {
    patchState(sessionStore, { isSessionPaused: true })
    fixture.detectChanges()

    const banner = fixture.nativeElement.querySelector('aside[role="alert"]')
    expect(banner).toBeTruthy()
    expect(banner?.textContent).toContain('EN PAUSE')
    expect(component['liveAnnouncement']()).toContain('Session en pause')
  })

  it('should display calm board reminder and submitted choice without score/ranking on reveal', () => {
    patchState(sessionStore, {
      mySubmittedChoice: { answerIndex: 2, label: 'Option C' },
      lastReveal: {
        slideIndex: 1,
        correctIndices: [1],
        explanation: 'Explication détaillée',
        stats: [],
      },
    })
    fixture.detectChanges()

    const text = fixture.nativeElement.textContent
    expect(text).toContain('Regarde le tableau')
    expect(text).toContain('Ta réponse enregistrée')
    expect(text).toContain('Option C')
    expect(text).toContain('Explication détaillée')
    expect(text).not.toContain('Classement :')
    expect(text).not.toContain('Points gagnés')
  })

  it('should display mobile correction indicator when mobileCorrectionAllowed is true', () => {
    patchState(sessionStore, {
      mySubmittedChoice: { answerIndex: 1, label: 'Bonne réponse' },
      lastReveal: {
        slideIndex: 1,
        correctIndices: [1],
        mobileCorrectionAllowed: true,
        stats: [],
      },
    })
    fixture.detectChanges()

    const text = fixture.nativeElement.textContent
    expect(text).toContain('Bonne réponse !')
  })
})
