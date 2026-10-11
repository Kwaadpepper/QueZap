import { InjectionToken } from '@angular/core'

import { ServiceObservable, ServiceState } from '@quezap/core/types'
import { MixedQuestion, Participant, QuestionId, QuestionWithAnswers } from '@quezap/domain/models'

export interface SessionWaitingStart {
  readonly type: 'SessionWaitingStart'
}

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

export interface SessionKicked {
  readonly type: 'SessionKicked'
  readonly participantName: string
}

export interface QuestionRevealData {
  readonly slideIndex: number
  readonly correctIndices: number[]
  readonly explanation?: string
  readonly stats: {
    readonly answerIndex: number
    readonly count: number
    readonly percentage: number
  }[]
}

export interface LeaderboardEntry {
  readonly rank: number
  readonly participantName: string
  readonly score: number
  readonly previousScore: number
  readonly rankDelta: number
}

export interface IntermediateLeaderboardData {
  readonly slideIndex: number
  readonly leaderboard: LeaderboardEntry[]
}

export interface SessionQuestionRevealed {
  readonly type: 'SessionQuestionRevealed'
  readonly reveal: QuestionRevealData
}

export interface SessionLeaderboardUpdated {
  readonly type: 'SessionLeaderboardUpdated'
  readonly data: IntermediateLeaderboardData
}

export type SessionEvent
  = | SessionWaitingStart
    | SessionStarted
    | SessionSwitchedQuestion
    | SessionEnded
    | SessionKicked
    | SessionQuestionRevealed
    | SessionLeaderboardUpdated

export type CurrentQuestion = WaitingQuestion | MixedQuestion & QuestionWithAnswers | NoMoreQuestions

export interface WaitingQuestion {
  readonly type: 'WaitingQuestion'
}

export interface NoMoreQuestions {
  readonly type: 'NoMoreQuestions'
}
export function sessionWaitingStart(session: SessionEvent): session is SessionWaitingStart {
  return session.type === 'SessionWaitingStart'
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
export function sessionQuestionRevealed(session: SessionEvent): session is SessionQuestionRevealed {
  return session.type === 'SessionQuestionRevealed'
}
export function sessionLeaderboardUpdated(session: SessionEvent): session is SessionLeaderboardUpdated {
  return session.type === 'SessionLeaderboardUpdated'
}
export function sessionKicked(session: SessionEvent): session is SessionKicked {
  return session.type === 'SessionKicked'
}

type AllQuestionTypes = MixedQuestion | WaitingQuestion | NoMoreQuestions

export function isMixedQuestion(question: AllQuestionTypes): question is MixedQuestion {
  return question.type !== 'WaitingQuestion' && question.type !== 'NoMoreQuestions'
}
export function isWaitingQuestion(question: AllQuestionTypes): question is WaitingQuestion {
  return question.type === 'WaitingQuestion'
}
export function isNoMoreQuestions(question: AllQuestionTypes): question is NoMoreQuestions {
  return question.type === 'NoMoreQuestions'
}

export interface SessionObserverService {
  sessionEvents(): ServiceObservable<SessionEvent>

  participants: ServiceState<Participant[]>

  question: ServiceState<CurrentQuestion>

  reveal: ServiceState<QuestionRevealData | undefined>

  leaderboard: ServiceState<IntermediateLeaderboardData | undefined>
}

export const SESSION_OBSERVER_SERVICE = new InjectionToken<SessionObserverService>('SessionObserverService')
