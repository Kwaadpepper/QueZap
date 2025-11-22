import { inject, Injectable } from '@angular/core'

import { delay, map, of, Subject, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { ServiceObservable } from '@quezap/core/types'
import {
  Answer, MixedQuestion, Participant, ParticipantId, PictureUrl,
  QuestionId, QuestionType, QuestionWithAnswers, Score, SessionCode,
  sessionHasEnded,
  sessionIsRunning, Theme, ThemeId,
} from '@quezap/domain/models'

import { SessionMocks } from '../session.mock'

import { NoMoreQuestions, SessionEvent, SessionObserverService } from './session-observer'

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

  readonly #themeNames = [
    'Arbres caducques', 'Fleurs sauvages', 'PLantes médicinales', 'Champignons comestibles',
    'Oiseaux communs', 'Insectes pollinisateurs', 'Mammifères nocturnes', 'Reptiles et amphibiens',
    'Fruits et légumes', 'Herbes aromatiques', 'Plantes aquatiques', 'Plantes carnivores',
  ]

  private readonly sessions = inject(SessionMocks)

  private readonly mockParticipants: Participant[] = Array
    .from({ length: Math.max(3, Math.random() * 25) })
    .map(() => this.generateRandomParticipant())

  private readonly sessionSubject = new Subject<SessionEvent>()
  private readonly questionSubject = new Subject<MixedQuestion & QuestionWithAnswers | NoMoreQuestions>()

  public participants(): ServiceObservable<Participant[]> {
    return of(this.mockParticipants).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error: participants')
        }
      }),
      map(participants => ({
        kind: 'success',
        result: participants,
      })),
    )
  }

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
          throw new ServiceError('Mock service error: session events')
        }
      }),
      map(event => ({
        kind: 'success',
        result: event,
      })),
    )
  }

  public questions(): ServiceObservable<MixedQuestion & QuestionWithAnswers | NoMoreQuestions> {
    return this.questionSubject.pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error: questions')
        }
      }),
      map(question => ({
        kind: 'success',
        result: question,
      })),
    )
  }

  // --- Specific to mock service ---

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

  private generateRandomParticipant(): Participant {
    const nickname = this.#participantsNames[Math.floor(Math.random() * this.#participantsNames.length)]
    return {
      id: crypto.randomUUID() as ParticipantId,
      nickname,
      score: 0 as Score,
    }
  }

  private generateRandomQuestion({
    type,
    theme,
  }: {
    type?: QuestionType
    theme?: Theme
  }): MixedQuestion & QuestionWithAnswers {
    const questionTypes = Object.values(QuestionType)
    const questionType = type ?? questionTypes[Math.floor(Math.random() * questionTypes.length)]
    const questionTheme = theme ?? this.generateRandomTheme()

    switch (questionType) {
      case QuestionType.Boolean:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Vrai ou faux ?' + Math.floor(Math.random() * 100),
          type: QuestionType.Boolean,
          limit: Math.random() < 0.5 ? { seconds: 30 } : undefined,
          theme: questionTheme,
          picture: Math.random() < 0.5 ? undefined : 'https://picsum.photos/400/300' as PictureUrl,
          answers: [],
        }
      case QuestionType.Binary:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Cette plante est plutot...' + Math.floor(Math.random() * 100),
          type: QuestionType.Binary,
          limit: Math.random() < 0.5 ? { seconds: 30 } : undefined,
          theme: questionTheme,
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
          theme: questionTheme,
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

  private generateRandomTheme(): Theme {
    const themes = Object.values(this.#themeNames).map(name => ({
      id: crypto.randomUUID() as ThemeId,
      name,
    }))

    return themes[Math.floor(Math.random() * themes.length)]
  }

  private queueQuestion(
    session: SessionCode,
    question: MixedQuestion & QuestionWithAnswers | NoMoreQuestions,
  ) {
    this.startSession(session)
    setTimeout(() => {
      this.questionSubject.next(question)
    }, this.MOCK_DELAY())
  }

  private startSession(code: SessionCode) {
    const session = this.sessions.getSessionByCode(code)
    if (!session) {
      throw new Error(`Session with code ${code} not found.`)
    }
    if (sessionIsRunning(session)) {
      return
    }
    if (sessionHasEnded(session)) {
      return
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
