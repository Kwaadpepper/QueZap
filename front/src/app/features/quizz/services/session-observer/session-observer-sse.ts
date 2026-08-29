import { inject, Injectable, signal, WritableSignal } from '@angular/core'

import { Observable, Subject } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceObservable, ServiceState, Tried } from '@quezap/core/types'
import {
  MixedQuestion, Participant, ParticipantId, PictureUrl,
  QuestionId, QuestionType, QuestionWithAnswers, Score,
} from '@quezap/domain/models'

import { CurrentQuestion, SessionEvent, SessionObserverService } from './session-observer'

interface SsePayloadParticipant {
  name: string
  score?: number
}

interface SsePayloadAnswer {
  id: string
  label: string
  picture?: string
}

interface SsePayload {
  participants?: SsePayloadParticipant[]
  correctAnswerIds?: string[]
  questionId?: string
  title?: string
  type?: string
  timerSeconds?: number
  points?: number
  picture?: string
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

  private eventSource: EventSource | null = null
  private readonly eventsSubject = new Subject<SessionEvent>()

  private readonly _participants: WritableSignal<Participant[]> = signal([])
  private readonly _question: WritableSignal<CurrentQuestion> = signal({ type: 'WaitingQuestion' })

  public readonly participants: ServiceState<Participant[]> = this._participants.asReadonly()
  public readonly question: ServiceState<CurrentQuestion> = this._question.asReadonly()

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

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

  private connect(): void {
    if (this.eventSource) {
      return
    }

    const sessionId = typeof sessionStorage !== 'undefined'
      ? sessionStorage.getItem('quezap_session_id') || 'active'
      : 'active'

    try {
      this.eventSource = new EventSource(`${this.apiUrl}/sessions/${sessionId}/events`)

      this.eventSource.onmessage = (event: MessageEvent) => {
        try {
          const envelope: SseEnvelope = JSON.parse(event.data)
          this.handleEvent(envelope)
        }
        catch (e) {
          console.debug('Failed to parse SSE payload', e)
        }
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
        const questionId = (payload.questionId || '018e3a2b-8888-7def-8123-abcdef888888') as QuestionId
        const questionObj: MixedQuestion & QuestionWithAnswers = {
          type: (payload.type as QuestionType) || QuestionType.Quizz,
          id: questionId,
          value: payload.title || 'Question',
          picture: payload.picture as PictureUrl | undefined,
          limit: { seconds: payload.timerSeconds || 30 },
          answers: (payload.answers || []).map((a, idx) => ({
            index: idx,
            points: payload.points || 1000,
            value: a.label,
            picture: a.picture as PictureUrl | undefined,
          })),
        }
        this._question.set(questionObj)
        this.eventsSubject.next({
          type: 'SessionSwitchedQuestion',
          questionId,
        })
        break
      }
      case 'QUESTION_REVEAL': {
        const current = this._question()
        if (current.type !== 'WaitingQuestion' && current.type !== 'NoMoreQuestions') {
          this._question.set({ ...current })
        }
        break
      }
      case 'SESSION_ENDED': {
        this._question.set({ type: 'NoMoreQuestions' })
        this.eventsSubject.next({
          type: 'SessionEnded',
          session: { endedAt: new Date(envelope.timestamp) },
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
  }
}
