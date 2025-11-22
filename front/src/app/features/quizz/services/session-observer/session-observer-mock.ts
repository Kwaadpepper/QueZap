import { inject, Injectable, signal } from '@angular/core'

import {
  BehaviorSubject,
  delay, map,
  tap,
} from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { ServiceObservable, ServiceState } from '@quezap/core/types'
import {
  Answer, MixedQuestion, Participant, ParticipantId, PictureUrl,
  QuestionId, QuestionType, QuestionWithAnswers, Score, SessionCode,
  sessionHasEnded,
  sessionHasStarted,
  Theme,
} from '@quezap/domain/models'

import { SessionMocks } from '../session.mock'

import {
  CurrentQuestion, isMixedQuestion,
  NoMoreQuestions,
  SessionEvent, SessionObserverService, WaitingQuestion,
} from './session-observer'

interface SessionRunState {
  session: SessionCode
  state: 'not_started' | 'running' | 'ended'
}

@Injectable()
export class SessionObserverMockService implements SessionObserverService {
  readonly #sessionRunStatePersistKey = 'mockSessionRunState'
  #sessionRunState: SessionRunState | undefined = undefined
  private readonly MOCK_ERROR = (failureProbability = 0.2) => Math.random() < failureProbability
  private readonly MOCK_DELAY = () => Math.max(1000, Math.random() * 3000)
  readonly #participantsNames = [
    'John', 'Jerry', 'Ivy', 'Jack', 'Kathy', 'Liam',
    'Mia', 'Noah', 'Olivia', 'Paul', 'Quinn', 'Ruby',
    'Sam', 'Tina', 'Uma', 'Vince', 'Wendy', 'Xander', 'Yara', 'Zane',
    'Alex', 'Bella', 'Carter', 'Diana', 'Ethan', 'Fiona',
    'George', 'Hannah', 'Ian', 'Jasmine', 'Kevin', 'Luna',
    'Mason', 'Nora', 'Owen', 'Piper', 'Quincy', 'Riley',
    'Sophia', 'Tyler', 'Ursula', 'Victor', 'Willow', 'Xenia',
    'Yusuf', 'Zara',
  ]

  private readonly sessions = inject(SessionMocks)

  private readonly mockQuestion = signal<CurrentQuestion>({ type: 'WaitingQuestion' })
  private readonly mockParticipantsState = signal<Participant[]>([])

  private readonly sessionSubject = new BehaviorSubject<SessionEvent>({ type: 'SessionWaitingStart' })

  public participants: ServiceState<Participant[]> = this.mockParticipantsState.asReadonly()
  public question: ServiceState<CurrentQuestion> = this.mockQuestion.asReadonly()

  public sessionEvents(): ServiceObservable<SessionEvent> {
    const previousState = this.getSessionRunState()

    switch (previousState?.state) {
      case 'running': this.startSession(previousState.session)
        break
      case 'ended': this.endSession(previousState.session)
    }

    return this.sessionSubject.pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          console.debug('Mock: error while session events')
          throw new ServiceError('Mock service error: session events')
        }
      }),
      map(event => ({
        kind: 'success',
        result: event,
      })),
    )
  }

  // --- Specific to mock service ---

  public mockSessionStart(onSession: SessionCode) {
    this.startSession(onSession)
  }

  public mockParticipants() {
    this.mockParticipantsState.set(Array
      .from({ length: Math.max(3, Math.random() * 25) })
      .map(() => this.generateRandomParticipant()))
  }

  public mockWaitingQuestion(onSession: SessionCode) {
    this.queueQuestion(onSession, { type: 'WaitingQuestion' })
  }

  public mockNextQuestion(onSession: SessionCode) {
    this.queueQuestion(
      onSession,
      this.generateRandomQuestion({}),
    )
  }

  public mockBooleanQuestion(onSession: SessionCode) {
    this.queueQuestion(
      onSession,
      this.generateRandomQuestion({ type: QuestionType.Boolean }),
    )
  }

  public mockBinaryQuestion(onSession: SessionCode) {
    this.queueQuestion(
      onSession,
      this.generateRandomQuestion({ type: QuestionType.Binary }),
    )
  }

  public mockQuizzQuestion(onSession: SessionCode) {
    this.queueQuestion(
      onSession,
      this.generateRandomQuestion({ type: QuestionType.Quizz }),
    )
  }

  public mockNoMoreQuestions(onSession: SessionCode) {
    this.queueQuestion(onSession, { type: 'NoMoreQuestions' })
    this.endSession(onSession)
  }

  // --- Internal generators ---

  private generateRandomParticipant(): Participant {
    const nickname = this.#participantsNames[Math.floor(Math.random() * this.#participantsNames.length)]
    return {
      id: crypto.randomUUID() as ParticipantId,
      nickname,
      score: 0 as Score,
    }
  }

  private generateRandomQuestion({ type }: {
    type?: QuestionType
    theme?: Theme
  }): MixedQuestion & QuestionWithAnswers {
    const questionTypes = Object.values(QuestionType)
    const questionType = type ?? questionTypes[Math.floor(Math.random() * questionTypes.length)]

    switch (questionType) {
      case QuestionType.Boolean:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Vrai ou faux ?' + Math.floor(Math.random() * 100),
          type: QuestionType.Boolean,
          limit: Math.random() < 0.5 ? { seconds: 30 } : undefined,
          picture: Math.random() < 0.5 ? undefined : 'https://picsum.photos/400/300' as PictureUrl,
          answers: [],
        }
      case QuestionType.Binary:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Cette plante est plutot...' + Math.floor(Math.random() * 100),
          type: QuestionType.Binary,
          limit: Math.random() < 0.5 ? { seconds: 30 } : undefined,
          picture: Math.random() < 0.5 ? undefined : 'https://picsum.photos/400/300' as PictureUrl,
          answers: [
            this.generateAnwer(0, QuestionType.Binary, 'Type A'),
            this.generateAnwer(1, QuestionType.Binary, 'Type B'),
          ],
        }
      case QuestionType.Quizz:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Quel est la bonne réponse ? ' + Math.floor(Math.random() * 100),
          type: QuestionType.Quizz,
          limit: Math.random() < 0.5 ? { seconds: 30 } : undefined,
          picture: Math.random() < 0.5 ? undefined : 'https://picsum.photos/400/300' as PictureUrl,
          answers: [
            this.generateAnwer(0, QuestionType.Quizz, 'Réponse A'),
            this.generateAnwer(1, QuestionType.Quizz, 'Réponse B'),
            this.generateAnwer(2, QuestionType.Quizz, 'Réponse C'),
            this.generateAnwer(3, QuestionType.Quizz, 'Réponse D'),
          ],
        }
      default:
        throw new Error('Unsupported question type')
    }
  }

  private generateAnwer(
    index: number,
    questionType: QuestionType,
    phrase?: string,
  ): Answer {
    let points = 0
    if (questionType === QuestionType.Boolean) {
      throw new Error('Boolean questions do not have answers')
    }
    else if (questionType === QuestionType.Binary) {
      points = Math.random() < 0.5 ? 1 : 0
    }
    else if (questionType === QuestionType.Quizz) {
      points = Math.random() < 0.25 ? 1 : 0
    }
    return {
      index,
      points,
      value: phrase,
      picture: phrase ? undefined : 'https://picsum.photos/400/300' as PictureUrl,
    }
  }

  // --- Internal methods ---

  private queueQuestion(
    code: SessionCode,
    question: WaitingQuestion | MixedQuestion & QuestionWithAnswers | NoMoreQuestions,
  ) {
    const session = this.sessions.getSessionByCode(code)
    if (!session) {
      throw new Error(`Session with code ${code} not found.`)
    }
    setTimeout(() => {
      console.debug('Mock: emitting question', question)
      this.mockQuestion.set(question)

      if (isMixedQuestion(question)) {
        console.debug('Mock: session switched question', question.id)
        this.sessionSubject.next({
          type: 'SessionSwitchedQuestion',
          questionId: question.id,
        })
      }
    }, this.MOCK_DELAY())
  }

  private startSession(code: SessionCode) {
    const session = this.sessions.getSessionByCode(code)
    if (!session) {
      throw new Error(`Session with code ${code} not found.`)
    }
    if (sessionHasStarted(session)) {
      throw new Error(`Session with code ${code} has already started.`)
    }
    if (sessionHasEnded(session)) {
      throw new Error(`Session with code ${code} has already ended.`)
    }
    this.sessions.startSession(code)
    setTimeout(() => {
      this.setSessionRunState({
        session: code,
        state: 'running',
      })
      this.sessionSubject.next({
        type: 'SessionStarted',
        session: { startedAt: new Date() },
      })
      console.debug('Mock: session started')
    }, this.MOCK_DELAY())
  }

  private endSession(session: SessionCode) {
    this.sessions.endSession(session)
    setTimeout(() => {
      this.setSessionRunState({
        session,
        state: 'ended',
      })
      this.sessionSubject.next({
        type: 'SessionEnded',
        session: { endedAt: new Date() },
      })
      console.debug('Mock: session ended')
    }, this.MOCK_DELAY())
  }

  private getSessionRunState() {
    try {
      const previousState = sessionStorage.getItem(this.#sessionRunStatePersistKey)
      if (previousState) {
        this.#sessionRunState = JSON.parse(previousState) as SessionRunState
      }
      return this.#sessionRunState
    }
    catch {
      return undefined
    }
  }

  private setSessionRunState(state: SessionRunState) {
    this.#sessionRunState = state
    sessionStorage.setItem(this.#sessionRunStatePersistKey, JSON.stringify(state))
  }
}
