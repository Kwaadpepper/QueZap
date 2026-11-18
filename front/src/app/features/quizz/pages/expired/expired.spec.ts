import { ComponentFixture, TestBed } from '@angular/core/testing'

import { SessionCode } from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../../services/active-session-persistence/active-session-persistence'

import { Expired } from './expired'

describe('Expired', () => {
  let component: Expired
  let fixture: ComponentFixture<Expired>
  let persistence: ActiveSessionPersistence

  beforeEach(async () => {
    localStorage.clear()
    sessionStorage.clear()

    await TestBed.configureTestingModule({
      imports: [Expired],
      providers: [ActiveSessionPersistence],
    }).compileComponents()

    persistence = TestBed.inject(ActiveSessionPersistence)
    persistence.persist({ code: '20082' as SessionCode, nickname: 'Léa' })
    persistence.saveParticipationData('session-expired', 'ptk_old', 'Léa')
    persistence.saveSubmittedChoice('q-1', { answerIndex: 0, label: 'A' }, 'session-expired')

    fixture = TestBed.createComponent(Expired)
    component = fixture.componentInstance
    await fixture.whenStable()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('should create and clean up expired session data while keeping remembered nickname', () => {
    expect(component).toBeTruthy()
    expect(persistence.getSessionId()).toBeNull()
    expect(persistence.getParticipationToken()).toBeNull()
    expect(persistence.getSubmittedChoice('q-1', 'session-expired')).toBeUndefined()
    expect(persistence.retrieve()?.code).toBe('')
    expect(persistence.retrieve()?.nickname).toBe('Léa')
  })
})
