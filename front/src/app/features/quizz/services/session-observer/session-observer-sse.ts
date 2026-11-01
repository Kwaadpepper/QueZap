import { inject, Injectable, signal, WritableSignal } from '@angular/core'

import { Observable, Subject } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceObservable, ServiceState, Tried } from '@quezap/core/types'
import {
  MixedQuestion, Participant, ParticipantId, PictureUrl,
  QuestionId, QuestionType, QuestionWithAnswers, Score,
} from '@quezap/domain/models'

import { ActiveSessionPersistence } from '../active-session-persistence/active-session-persistence'

import {
  CurrentQuestion,
  IntermediateLeaderboardData,
  QuestionRevealData,
  SessionEvent,
  SessionObserverService,
} from './session-observer'

interface SsePayloadParticipant {
  name: string
  score?: number
}

interface SsePayloadAnswer {
  id?: string
  index?: number
  label?: string
  value?: string
  picture?: string
  pictureUrl?: string
  pictureFilename?: string
}

interface SsePayloadQuestion {
  id?: string
  type?: string
  label?: string
  picture?: string
  pictureUrl?: string
  pictureFilename?: string
  timerSeconds?: number
  answers?: SsePayloadAnswer[]
}

interface SsePayload {
  id?: string
  participantName?: string
  participants?: SsePayloadParticipant[]
  correctAnswerIds?: string[]
  correctAnswerIndices?: number[]
  questionId?: string
  title?: string
  label?: string
  type?: string
  timerSeconds?: number
  remainingSeconds?: number
  deadline?: string
  answeredCount?: number
  slideIndex?: number
  totalSlides?: number
  points?: number
  picture?: string
  pictureUrl?: string
  pictureFilename?: string
  question?: SsePayloadQuestion
  theme?: {
    id: string
    name: string
  }
  answers?: SsePayloadAnswer[]
}

interface SseEnvelope {
  type: string
  sessionNumber: number
  sessionId: string
  timestamp: string
  payload?: SsePayload
}

@Injectable({ providedIn: 'root' })
export class SessionObserverSseService implements SessionObserverService {
  private readonly config = inject(Config)
  private readonly sessionPersistence = inject(ActiveSessionPersistence)

  private eventSource: EventSource | null = null
  private readonly eventsSubject = new Subject<SessionEvent>()

  private readonly _participants: WritableSignal<Participant[]> = signal([])
  private readonly _question: WritableSignal<CurrentQuestion> = signal({ type: 'WaitingQuestion' })
  private readonly _reveal: WritableSignal<QuestionRevealData | undefined> = signal(undefined)
  private readonly _leaderboard: WritableSignal<IntermediateLeaderboardData | undefined> = signal(undefined)

  public readonly participants: ServiceState<Participant[]> = this._participants.asReadonly()
  public readonly question: ServiceState<CurrentQuestion> = this._question.asReadonly()
  public readonly reveal: ServiceState<QuestionRevealData | undefined> = this._reveal.asReadonly()
  public readonly leaderboard: ServiceState<IntermediateLeaderboardData | undefined> = this._leaderboard.asReadonly()

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  private currentConnectedSessionId: string | null = null

  sessionEvents(): ServiceObservable<SessionEvent> {
    this.connect()
    return new Observable<Tried<SessionEvent, ServiceError>>((subscriber) => {
      const subscription = this.eventsSubject.subscribe({
        next: val => subscriber.next({ kind: 'success', result: val }),
        error: err => subscriber.next({ kind: 'failure', error: new ServiceError(err.message || 'SSE error') }),
      })
      return () => subscription.unsubscribe()
    })
  }

  public connect(customSessionId?: string): void {
    const sessionId = customSessionId || this.sessionPersistence.getSessionId()
    if (!sessionId || sessionId === 'active') {
      console.debug('No valid sessionId available yet for SSE connection')
      return
    }

    if (this.eventSource) {
      if (this.currentConnectedSessionId === sessionId) {
        return
      }
      this.disconnect()
    }

    this.currentConnectedSessionId = sessionId

    try {
      this.eventSource = new EventSource(`${this.apiUrl}/sessions/${sessionId}/events?ngsw-bypass=true`)

      const parseAndHandle = (data: string) => {
        try {
          const envelope: SseEnvelope = JSON.parse(data)
          this.handleEvent(envelope)
        }
        catch (e) {
          console.debug('Failed to parse SSE payload', e)
        }
      }

      this.eventSource.onmessage = (event: MessageEvent) => {
        parseAndHandle(event.data)
      }

      const eventNames = [
        'LOBBY_STATE',
        'SESSION_STARTED',
        'QUESTION_START',
        'QUESTION_TIME_WARNING',
        'QUESTION_REVEAL',
        'INTERMEDIATE_LEADERBOARD',
        'SESSION_ENDED',
        'PARTICIPANT_KICKED',
        'SESSION_PAUSE_STATUS',
      ]

      for (const name of eventNames) {
        this.eventSource.addEventListener(name, (e: MessageEvent) => {
          parseAndHandle(e.data)
        })
      }

      this.eventSource.onerror = (error) => {
        console.debug('SSE connection error, auto-retrying...', error)
      }
    }
    catch (e) {
      console.debug('Could not create EventSource', e)
    }
  }

  private handleEvent(envelope: SseEnvelope): void {
    switch (envelope.type) {
      case 'LOBBY_STATE': {
        if (envelope.payload?.participants) {
          const participantsList: Participant[] = envelope.payload.participants.map((p, idx) => ({
            id: `018e3a2b-8888-7def-8123-00000000000${idx}` as ParticipantId,
            nickname: p.name,
            score: (p.score || 0) as Score,
          }))
          this._participants.set(participantsList)
        }
        this.eventsSubject.next({ type: 'SessionWaitingStart' })
        break
      }
      case 'SESSION_STARTED': {
        this.eventsSubject.next({
          type: 'SessionStarted',
          session: { startedAt: new Date(envelope.timestamp) },
        })
        break
      }
      case 'QUESTION_START': {
        const payload = envelope.payload || {}
        const q = payload.question || payload
        const questionId = (q.id || payload.questionId || '018e3a2b-8888-7def-8123-abcdef888888') as QuestionId
        const rawAnswers = q.answers || payload.answers || []
        const seconds = payload.remainingSeconds ?? (payload.deadline
          ? Math.max(1, Math.round((new Date(payload.deadline).getTime() - Date.now()) / 1000))
          : (payload.timerSeconds || q.timerSeconds || 30))
        const questionObj: MixedQuestion & QuestionWithAnswers = {
          type: (q.type as QuestionType) || (payload.type as QuestionType) || QuestionType.Quizz,
          id: questionId,
          value: q.label || payload.title || payload.label || 'Question',
          picture: (q.pictureUrl
            || q.pictureFilename
            || q.picture
            || payload.pictureUrl
            || payload.picture) as PictureUrl | undefined,
          limit: { seconds, deadline: payload.deadline },
          answers: rawAnswers.map((a, idx) => ({
            index: typeof a.index === 'number' ? a.index : idx,
            points: payload.points || 1000,
            value: a.label || a.value || '',
            picture: (a.pictureUrl || a.pictureFilename || a.picture) as PictureUrl | undefined,
          })),
        }
        this._reveal.set(undefined)
        this._question.set(questionObj)
        this.eventsSubject.next({
          type: 'SessionSwitchedQuestion',
          questionId,
        })
        break
      }
      case 'QUESTION_REVEAL': {
        const payload = (envelope.payload || {}) as {
          slideIndex?: number
          correctAnswerIndices?: number[]
          correctIndices?: number[]
          acceptedAnswers?: string[]
          explanation?: string
          mobileCorrectionAllowed?: boolean
          answersStats?: { answerIndex: number, count: number, percentage: number }[]
          stats?: { answerIndex: number, count: number, percentage: number }[]
        }
        const revealData: QuestionRevealData = {
          slideIndex: payload.slideIndex ?? 0,
          correctIndices: payload.correctAnswerIndices || payload.correctIndices || [],
          acceptedAnswers: payload.acceptedAnswers,
          explanation: payload.explanation,
          mobileCorrectionAllowed: payload.mobileCorrectionAllowed,
          stats: (payload.answersStats || payload.stats || []).map(s => ({
            answerIndex: s.answerIndex ?? 0,
            count: s.count ?? 0,
            percentage: s.percentage ?? 0,
          })),
        }
        this._reveal.set(revealData)
        this.eventsSubject.next({
          type: 'SessionQuestionRevealed',
          reveal: revealData,
        })
        break
      }
      case 'INTERMEDIATE_LEADERBOARD': {
        const payload = (envelope.payload || {}) as {
          slideIndex?: number
          leaderboard?: {
            rank: number
            participantName: string
            score: number
            delta?: number
            previousScore?: number
            rankDelta?: number
          }[]
        }
        const lbData: IntermediateLeaderboardData = {
          slideIndex: payload.slideIndex ?? 0,
          leaderboard: (payload.leaderboard || []).map(l => ({
            rank: l.rank,
            participantName: l.participantName,
            score: l.score,
            delta: l.delta ?? (l.previousScore !== undefined ? Math.max(0, l.score - l.previousScore) : 0),
            previousScore: l.previousScore !== undefined ? l.previousScore : (l.score - (l.delta ?? 0)),
            rankDelta: l.rankDelta ?? 0,
          })),
        }
        this._leaderboard.set(lbData)
        this.eventsSubject.next({
          type: 'SessionLeaderboardUpdated',
          data: lbData,
        })
        break
      }
      case 'SESSION_ENDED': {
        this._question.set({ type: 'NoMoreQuestions' })
        const payload = envelope.payload as {
          leaderboard?: {
            rank: number
            participantName: string
            score?: number
            totalScore?: number
            correctAnswers?: number
            correctCount?: number
          }[]
          finalLeaderboard?: {
            rank: number
            participantName: string
            score?: number
            totalScore?: number
            correctAnswers?: number
            correctCount?: number
          }[]
          metrics?: {
            totalParticipants?: number
            averageScore?: number
            globalSuccessRate?: number
          }
          podium?: { first: string, second?: string, third?: string }
        } | undefined
        const leaderboardList = payload?.finalLeaderboard || payload?.leaderboard
        if (leaderboardList) {
          const myNickname = this.sessionPersistence.getParticipantName() || ''
          const myEntry = leaderboardList.find(
            e => e.participantName.trim().toLowerCase() === myNickname.trim().toLowerCase(),
          )
          const finalScore = myEntry ? (myEntry.totalScore ?? myEntry.score ?? 0) : 0
          const correctCount = myEntry ? (myEntry.correctCount ?? myEntry.correctAnswers ?? 0) : 0
          const classAvg = payload?.metrics?.averageScore
            ?? (leaderboardList.length > 0
              ? Math.round(
                  leaderboardList.reduce((acc, cur) => acc + (cur.totalScore ?? cur.score ?? 0), 0)
                  / leaderboardList.length,
                )
              : 0)

          this.sessionPersistence.saveFinalResults({
            rank: myEntry ? myEntry.rank : leaderboardList.length + 1,
            totalParticipants: payload?.metrics?.totalParticipants ?? leaderboardList.length,
            score: finalScore,
            correctAnswers: correctCount,
            isPodium: myEntry ? myEntry.rank <= 3 : false,
            classAverageScore: classAvg,
            classSuccessRate: payload?.metrics?.globalSuccessRate ?? 0,
            fullLeaderboard: leaderboardList.map(l => ({
              rank: l.rank,
              participantName: l.participantName,
              score: l.totalScore ?? l.score ?? 0,
              correctCount: l.correctCount ?? l.correctAnswers,
            })),
          })
        }
        this.eventsSubject.next({
          type: 'SessionEnded',
          session: { endedAt: new Date(envelope.timestamp) },
        })
        break
      }
      case 'SESSION_PAUSE_STATUS': {
        const payload = envelope.payload as { isPaused?: boolean } | undefined
        if (payload?.isPaused !== undefined) {
          this.eventsSubject.next({
            type: 'SessionPauseStatus',
            isPaused: payload.isPaused,
          })
        }
        break
      }
      case 'PARTICIPANT_KICKED': {
        const payload = envelope.payload || {}
        this.eventsSubject.next({
          type: 'SessionKicked',
          participantName: payload.participantName || '',
        })
        break
      }
      default:
        break
    }
  }

  public disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    this.currentConnectedSessionId = null
    this._participants.set([])
    this._question.set({ type: 'WaitingQuestion' })
    this._reveal.set(undefined)
    this._leaderboard.set(undefined)
  }
}
