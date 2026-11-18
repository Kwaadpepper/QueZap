import { ComponentFixture, TestBed } from '@angular/core/testing'

import { of } from 'rxjs'

import { NotFoundError } from '@quezap/core/errors'

import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'
import { SESSION_API_SERVICE, SessionApiService } from '../../services/session-api/session-api'

import { Ended } from './ended'

describe('Ended', () => {
  let component: Ended
  let fixture: ComponentFixture<Ended>
  let persistence: ActiveSessionPersistence

  beforeEach(async () => {
    localStorage.clear()
    sessionStorage.clear()

    await TestBed.configureTestingModule({
      imports: [Ended],
      providers: [ActiveSessionPersistence],
    }).compileComponents()

    persistence = TestBed.inject(ActiveSessionPersistence)
    fixture = TestBed.createComponent(Ended)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should display podium badge and statistics when player is on podium', () => {
    persistence.saveFinalResults({
      rank: 1,
      totalParticipants: 35,
      score: 906,
      correctAnswers: 3,
      isPodium: true,
    })
    fixture.detectChanges()

    const compiled = fixture.nativeElement as HTMLElement
    expect(compiled.textContent).toContain('PODIUM OFFICIEL !')
    expect(compiled.textContent).toContain('🥇')
    expect(compiled.textContent).toContain('#1')
    expect(compiled.textContent).toContain('sur 35')
    expect(compiled.textContent).toContain('906')
    expect(compiled.textContent).toContain('3')
    expect(compiled.textContent).toContain('bonnes rép.')
  })

  it('should display normal completion with rank statistics when player is not on podium', () => {
    persistence.saveFinalResults({
      rank: 7,
      totalParticipants: 20,
      score: 450,
      correctAnswers: 2,
      isPodium: false,
    })
    fixture.detectChanges()

    const compiled = fixture.nativeElement as HTMLElement
    expect(compiled.textContent).toContain('Partie terminée')
    expect(compiled.textContent).not.toContain('PODIUM OFFICIEL !')
    expect(compiled.textContent).toContain('#7')
    expect(compiled.textContent).toContain('sur 20')
    expect(compiled.textContent).toContain('450')
  })

  it('should display collective class benchmark and full leaderboard', () => {
    jest.spyOn(persistence, 'getParticipantName').mockReturnValue('Charlie')
    persistence.saveFinalResults({
      rank: 2,
      totalParticipants: 3,
      score: 800,
      correctAnswers: 2,
      isPodium: true,
      classAverageScore: 750,
      classSuccessRate: 67,
      fullLeaderboard: [
        { rank: 1, participantName: 'Alice', score: 950 },
        { rank: 2, participantName: 'Charlie', score: 800 },
        { rank: 3, participantName: 'Bob', score: 500 },
      ],
    })
    fixture.detectChanges()

    const compiled = fixture.nativeElement as HTMLElement
    expect(compiled.textContent).toContain('Dynamique collective de la classe')
    expect(compiled.textContent).toContain('750 pts')
    expect(compiled.textContent).toContain('67%')
    expect(compiled.textContent).toContain('Classement complet de la classe (3)')
    expect(compiled.textContent).toContain('Alice')
    expect(compiled.textContent).toContain('Charlie')
    expect(compiled.textContent).toContain('(Toi)')
    expect(compiled.textContent).toContain('Bob')
    expect(compiled.textContent).toContain('Bravo pour ton apport au résultat de la classe !')
  })

  it('should fallback to /apiv1/sessions/{id}/scores when cache is empty', async () => {
    const mockApi: Partial<SessionApiService> = {
      getSessionScores: jest.fn().mockReturnValue(of({
        kind: 'success' as const,
        result: {
          sessionId: 'session-fallback',
          slideIndex: 3,
          leaderboard: [
            { rank: 1, participantName: 'Alice', score: 1200 },
            { rank: 2, participantName: 'Léa', score: 800 },
          ],
        },
      })),
    }

    TestBed.resetTestingModule()
    await TestBed.configureTestingModule({
      imports: [Ended],
      providers: [
        ActiveSessionPersistence,
        { provide: SESSION_API_SERVICE, useValue: mockApi },
      ],
    }).compileComponents()

    const freshPersistence = TestBed.inject(ActiveSessionPersistence)
    freshPersistence.saveParticipationData('session-fallback', 'ptk_1', 'Léa')

    const freshFixture = TestBed.createComponent(Ended)
    freshFixture.detectChanges()
    await freshFixture.whenStable()

    const compiled = freshFixture.nativeElement as HTMLElement
    expect(mockApi.getSessionScores).toHaveBeenCalledWith('session-fallback')
    expect(compiled.textContent).toContain('#2')
    expect(compiled.textContent).toContain('800')
    expect(compiled.textContent).toContain('1000 pts')
    expect(compiled.textContent).toContain('Classement complet de la classe (2)')
  })

  it('should render empty state cleanly when /scores fallback fails', async () => {
    const mockApi: Partial<SessionApiService> = {
      getSessionScores: jest.fn().mockReturnValue(of({
        kind: 'failure' as const,
        error: new NotFoundError('Not found'),
      })),
    }

    TestBed.resetTestingModule()
    await TestBed.configureTestingModule({
      imports: [Ended],
      providers: [
        ActiveSessionPersistence,
        { provide: SESSION_API_SERVICE, useValue: mockApi },
      ],
    }).compileComponents()

    const freshPersistence = TestBed.inject(ActiveSessionPersistence)
    freshPersistence.saveParticipationData('session-missing', 'ptk_1', 'Léa')

    const freshFixture = TestBed.createComponent(Ended)
    freshFixture.detectChanges()
    await freshFixture.whenStable()

    const compiled = freshFixture.nativeElement as HTMLElement
    expect(compiled.textContent).toContain('Bravo, Léa !')
    expect(compiled.textContent).toContain('Rejoindre une autre partie')
  })
})
