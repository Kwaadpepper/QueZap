import { Injectable } from '@angular/core'

import { Session, SessionCode, SessionId } from '@quezap/domain/models'

const MOCK_SESSIONS: Session[] = [
  {
    id: '019a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'A2B3C4' as SessionCode,
    name: 'Les arbres caduques',
    startedAt: null,
    endedAt: null,
  },
  {
    id: '129a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'D5E6F7' as SessionCode,
    name: 'Les végetaux aquatiques',
    startedAt: null,
    endedAt: null,
  },
  {
    id: '239a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'G8H9I0' as SessionCode,
    name: 'Les fleurs sauvages',
    startedAt: null,
    endedAt: null,
  },
  {
    id: '339a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'J1K2L3' as SessionCode,
    name: 'Les plantes médicinales',
    startedAt: null,
    endedAt: null,
  },
  {
    id: '439a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'M4N5O6' as SessionCode,
    name: 'Les arbres fruitiers',
    startedAt: null,
    endedAt: null,
  },
]

@Injectable()
export class SessionMocks {
  private readonly sessions = new Map<SessionCode, Session>(
    MOCK_SESSIONS.map(session => [session.code, session]),
  )

  public getSessions(): Session[] {
    return Array.from(this.sessions.values())
  }

  public getSessionByCode(code: SessionCode): Session | undefined {
    return this.sessions.get(code)
  }

  public startSession(code: SessionCode): void {
    const session = this.sessions.get(code)
    if (!session) {
      throw new Error(`Session with code ${code} not found.`)
    }
    if (session.startedAt !== null) {
      throw new Error(`Session with code ${code} has already started.`)
    }

    this.sessions.set(code, {
      ...session,
      startedAt: new Date(),
    })
  }

  public endSession(code: SessionCode): void {
    const session = this.sessions.get(code)
    if (!session) {
      throw new Error(`Session with code ${code} not found.`)
    }
    if (session.endedAt !== null) {
      throw new Error(`Session with code ${code} has already ended.`)
    }

    this.sessions.set(code, {
      ...session,
      endedAt: new Date(),
    })
  }
}
