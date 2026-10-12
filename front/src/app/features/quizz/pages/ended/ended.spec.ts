import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'

import { Ended } from './ended'

describe('Ended', () => {
  let component: Ended
  let fixture: ComponentFixture<Ended>
  let persistence: ActiveSessionPersistence

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Ended],
      providers: [ActiveSessionPersistence],
    }).compileComponents()

    persistence = TestBed.inject(ActiveSessionPersistence)
    fixture = TestBed.createComponent(Ended)
    component = fixture.componentInstance
    await fixture.whenStable()
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
})
