import { UUID } from '../types'

export type SessionId = UUID & {
  readonly __type: 'Session'
}

export interface Session {
  readonly id: SessionId
  readonly code: SessionCode
  readonly name: string
  readonly status?: 'LOBBY' | 'STARTED' | 'ENDED'
  readonly startedAt: Date | null
  readonly endedAt: Date | null
}

export type SessionCode = string & {
  readonly __type: 'SessionCode'
}

export function isValidSessionCode(code: string): code is SessionCode {
  if (!code) return false
  const digits = code.replace(/\D/g, '')
  return digits.length >= 3 && digits.length <= 12
}

export function sessionMayStart(session: Session): boolean {
  return session.status === 'LOBBY' || (session.startedAt === null && session.endedAt === null)
}

export function sessionHasStarted(session: Session): boolean {
  return session.status === 'STARTED' || (session.startedAt !== null && session.endedAt === null)
}

export function sessionHasEnded(session: Session): boolean {
  return session.status === 'ENDED' || session.endedAt !== null
}
