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
        correctAnswers: [0],
        stats: [],
      },
    })
    fixture.detectChanges()

    expect(clearTimerSpy).toHaveBeenCalled()
  })
})
