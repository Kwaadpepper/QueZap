import { InjectionToken } from '@angular/core'

import { ServiceObservable } from '@quezap/core/types'
import { MixedQuestion, Participant, QuestionId, QuestionWithAnswers } from '@quezap/domain/models'

export interface SessionStarted {
  readonly type: 'SessionStarted'
  readonly session: {
    readonly startedAt: Date
  }
}

export interface SessionEnded {
  readonly type: 'SessionEnded'
  readonly session: {
    readonly endedAt: Date
  }
}

export interface SessionSwitchedQuestion {
  readonly type: 'SessionSwitchedQuestion'
  readonly questionId: QuestionId
}

export type SessionEvent = SessionStarted | SessionSwitchedQuestion | SessionEnded

export interface NoMoreQuestions {
  readonly type: 'NoMoreQuestions'
}

export function sessionStarted(session: SessionEvent): session is SessionStarted {
  return session.type === 'SessionStarted'
}
export function sessionEnded(session: SessionEvent): session is SessionEnded {
  return session.type === 'SessionEnded'
}
export function sessionSwitchedQuestion(session: SessionEvent): session is SessionSwitchedQuestion {
  return session.type === 'SessionSwitchedQuestion'
}
export function isNoMoreQuestions(question: MixedQuestion | NoMoreQuestions): question is NoMoreQuestions {
  return question.type === 'NoMoreQuestions'
}

export interface SessionObserverService {
  sessionEvents(): ServiceObservable<SessionEvent>

  participants(): ServiceObservable<Participant[]>

  questions(): ServiceObservable<MixedQuestion & QuestionWithAnswers | NoMoreQuestions>
}

export const SESSION_OBSERVER_SERVICE = new InjectionToken<SessionObserverService>('SessionObserverService')
