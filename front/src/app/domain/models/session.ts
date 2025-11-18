import { UUID } from '../types'

export type SessionId = UUID & {
  readonly __type: 'Session'
}

export interface Session {
  readonly id: SessionId
  readonly code: SessionCode
  readonly name: string
  readonly startedAt: Date | null
  readonly endedAt: Date | null
}

export type SessionCode = string & {
  readonly __type: 'SessionCode'
}

export function isValidSessionCode(code: string): code is SessionCode {
  const sessionCodeRegex = /^[A-Z0-9]{6}$/
  return sessionCodeRegex.test(code)
}

export function sessionMayStart(session: Session): boolean {
  return session.startedAt === null && session.endedAt === null
}

export function sessionIsRunning(session: Session): boolean {
  return session.startedAt !== null && session.endedAt === null
}

export function sessionHasEnded(session: Session): boolean {
  return session.endedAt !== null
}
