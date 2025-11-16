import { UUID } from '../types'

export type SessionId = UUID & {
  readonly __type: 'Session'
}

export interface Session {
  readonly id: SessionId
  readonly code: SessionCode
  readonly name: string
}

export type SessionCode = string & {
  readonly __type: 'SessionCode'
}

export function isValidSessionCode(code: string): code is SessionCode {
  const sessionCodeRegex = /^[A-Z0-9]{6}$/
  return sessionCodeRegex.test(code)
}
