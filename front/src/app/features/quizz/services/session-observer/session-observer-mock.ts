import { Injectable } from '@angular/core'

import { delay, map, of, Subject, tap } from 'rxjs'

import { ServiceError } from '@quezap/core/errors'
import { ServiceObservable } from '@quezap/core/types'
import { MixedQuestion, Participant, ParticipantId, QuestionId, QuestionType, Score, Theme, ThemeId } from '@quezap/domain/models'

import { NoMoreQuestions, SessionEvent, SessionObserverService } from './session-observer'

@Injectable()
export class SessionObserverMockService implements SessionObserverService {
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

  private readonly mockParticipants: Participant[] = Array.from({
    length: Math.max(3, Math.random() * 25),
  }).map(() => this.generateRandomParticipant())

  private readonly sessionSubject = new Subject<SessionEvent>()
  private readonly questionSubject = new Subject<MixedQuestion | NoMoreQuestions>()

  public participants(): ServiceObservable<Participant[]> {
    return of(this.mockParticipants).pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map(participants => ({
        kind: 'success',
        result: participants,
      })),
    )
  }

  public sessionEvents(): ServiceObservable<SessionEvent> {
    return this.sessionSubject.pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map(event => ({
        kind: 'success',
        result: event,
      })),
    )
  }

  public questions(): ServiceObservable<MixedQuestion | NoMoreQuestions> {
    return this.questionSubject.pipe(
      delay(this.MOCK_DELAY()),
      tap(() => {
        if (this.MOCK_ERROR()) {
          throw new ServiceError('Mock service error')
        }
      }),
      map(question => ({
        kind: 'success',
        result: question,
      })),
    )
  }

  // --- Specific to mock service ---

  public mockNextQuestion() {
    this.queueQuestion(this.generateRandomQuestion({}))
  }

  public mockBooleanQuestion() {
    this.queueQuestion(this.generateRandomQuestion({
      type: QuestionType.Boolean,
    }))
  }

  public mockBinaryQuestion() {
    this.queueQuestion(this.generateRandomQuestion({
      type: QuestionType.Binary,
    }))
  }

  public mockQuizzQuestion() {
    this.queueQuestion(this.generateRandomQuestion({
      type: QuestionType.Quizz,
    }))
  }

  public mockNoMoreQuestions() {
    this.queueQuestion({ type: 'NoMoreQuestions' })
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
  }): MixedQuestion {
    const questionTypes = Object.values(QuestionType)
    const questionType = type ?? questionTypes[Math.floor(Math.random() * questionTypes.length)]
    const questionTheme = theme ?? this.generateRandomTheme()

    switch (questionType) {
      case QuestionType.Boolean:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Vrai ou faux ?',
          type: QuestionType.Boolean,
          theme: questionTheme,
        }
      case QuestionType.Binary:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Cette plante est plutot...',
          type: QuestionType.Binary,
          theme: questionTheme,
        }
      case QuestionType.Quizz:
        return {
          id: crypto.randomUUID() as QuestionId,
          value: 'Quel est la bonne réponse ?',
          type: QuestionType.Quizz,
          theme: questionTheme,
        }
      default:
        throw new Error('Unsupported question type')
    }
  }

  private generateRandomTheme(): Theme {
    const themes = Object.values(this.#themeNames).map(name => ({
      id: crypto.randomUUID() as ThemeId,
      name,
    }))

    return themes[Math.floor(Math.random() * themes.length)]
  }

  private queueQuestion(question: MixedQuestion | NoMoreQuestions) {
    this.startSession()
    setTimeout(() => {
      this.questionSubject.next(question)
    }, this.MOCK_DELAY())
  }

  private startSession() {
    setTimeout(() => {
      this.sessionSubject.next({
        type: 'SessionStarted',
        session: {
          startedAt: new Date(),
        },
      })
    }, this.MOCK_DELAY())
  }

  private endSession() {
    setTimeout(() => {
      this.sessionSubject.next({
        type: 'SessionEnded',
        session: {
          endedAt: new Date(),
        },
      })
    }, this.MOCK_DELAY())
  }
}
