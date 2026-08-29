import {
  ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, signal, WritableSignal,
} from '@angular/core'
import { ActivatedRoute } from '@angular/router'

import { Config } from '@quezap/core/services/config/config'

import { SESSION_SERVICE, SessionApiResponse, ScoreResponse } from '../../services/session'

export type PresenterStage = 'LOBBY' | 'QUESTION' | 'REVEAL' | 'PODIUM'

interface QuestionPresenterModel {
  id: string
  title: string
  answers: {
    id: string
    label: string
    isCorrect: boolean
  }[]
}

interface LeaderboardItem {
  name: string
  score: number
}

// SSE interfaces
interface SseParticipant {
  name: string
}

interface SseAnswer {
  id: string
  label: string
}

interface SsePayload {
  participants?: SseParticipant[]
  timerSeconds?: number
  questionId?: string
  title?: string
  answers?: SseAnswer[]
  correctAnswerIds?: string[]
}

interface SseEnvelope {
  type: string
  payload?: SsePayload
}

@Component({
  selector: 'quizz-session-presenter',
  templateUrl: './session-presenter.html',
  styleUrls: ['./session-presenter.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionPresenter implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly config = inject(Config)

  protected readonly sessionId = signal<string>('')
  protected readonly sessionCode = signal<string>('')
  protected readonly sessionName = signal<string>('')
  protected readonly stage: WritableSignal<PresenterStage> = signal('LOBBY')

  protected readonly participants = signal<{ name: string }[]>([])

  protected readonly currentQuestionIndex = signal(0)
  protected readonly totalQuestions = signal(1)
  protected readonly timer = signal(30)
  protected readonly answersReceived = signal(0)

  protected readonly currentQuestion = signal<QuestionPresenterModel>({
    id: '',
    title: '',
    answers: [],
  })

  protected readonly leaderboard = signal<LeaderboardItem[]>([])

  private eventSource: EventSource | null = null

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '018e3a2b-8888-7def-8123-abcdef888888'
    this.sessionId.set(id)

    this.sessionService.getSession(id).subscribe((session: SessionApiResponse) => {
      this.sessionCode.set(session.sessionCode || '')
      this.sessionName.set(session.name || '')
      if (session.status === 'STARTED') {
        this.stage.set('QUESTION')
      }
      else if (session.status === 'ENDED') {
        this.stage.set('PODIUM')
        this.fetchScores()
      }
    })

    this.connectSse(id)
  }

  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close()
    }
  }

  private connectSse(id: string): void {
    const apiUrl = this.config.appConfig.value().apiUrl
    this.eventSource = new EventSource(`${apiUrl}/sessions/${id}/events`)

    this.eventSource.onmessage = (event) => {
      try {
        const envelope: SseEnvelope = JSON.parse(event.data)
        this.handleEvent(envelope)
      }
      catch (e) {
        console.error(e)
      }
    }
  }

  private handleEvent(envelope: SseEnvelope): void {
    switch (envelope.type) {
      case 'LOBBY_STATE':
        if (envelope.payload?.participants) {
          this.participants.set(envelope.payload.participants.map((p: SseParticipant) => ({ name: p.name })))
        }
        break
      case 'SESSION_STARTED':
        this.stage.set('QUESTION')
        break
      case 'QUESTION_START': {
        const payload = envelope.payload
        if (payload) {
          this.stage.set('QUESTION')
          this.answersReceived.set(0)
          if (payload.timerSeconds) this.timer.set(payload.timerSeconds)
          this.currentQuestion.set({
            id: payload.questionId || '',
            title: payload.title || '',
            answers: (payload.answers || []).map((a: SseAnswer) => ({
              id: a.id || a.label,
              label: a.label,
              isCorrect: false,
            })),
          })
        }
        break
      }
      case 'QUESTION_REVEAL':
        this.stage.set('REVEAL')
        if (envelope.payload?.correctAnswerIds) {
          const correctIds = envelope.payload.correctAnswerIds
          this.currentQuestion.update(q => ({
            ...q,
            answers: q.answers.map(a => ({
              ...a,
              isCorrect: correctIds.includes(a.id),
            })),
          }))
        }
        break
      case 'SESSION_ENDED':
        this.stage.set('PODIUM')
        this.fetchScores()
        break
    }
  }

  private fetchScores(): void {
    this.sessionService.getScores(this.sessionId()).subscribe((scores: ScoreResponse[]) => {
      this.leaderboard.set(scores.map((s: ScoreResponse) => ({ name: s.name, score: s.score })))
    })
  }

  protected startQuiz(): void {
    this.sessionService.startSession(this.sessionId()).subscribe()
  }

  protected revealAnswers(): void {
    this.stage.set('REVEAL')
  }

  protected nextStep(): void {
    this.sessionService.nextQuestion(this.sessionId()).subscribe()
  }

  protected isLastQuestion(): boolean {
    return this.currentQuestionIndex() + 1 >= this.totalQuestions()
  }

  protected kick(name: string): void {
    this.sessionService.kickParticipant(this.sessionId(), name).subscribe()
  }

  protected toggleFullscreen(): void {
    if (typeof document !== 'undefined') {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(() => void 0)
      }
      else {
        document.exitFullscreen().catch(() => void 0)
      }
    }
  }

  protected readonly confettiPieces = Array.from({ length: 40 }, (_, i) => ({
    id: i,
    left: `${(i * 2.5) % 100}%`,
    delay: `${(i * 0.12) % 3}s`,
    duration: `${2.5 + ((i * 0.3) % 2)}s`,
    color: ['#ef4444', '#3b82f6', '#eab308', '#22c55e', '#ec4899', '#a855f7', '#06b6d4', '#fbbf24'][i % 8],
    width: `${8 + (i % 6)}px`,
    height: `${12 + (i % 8)}px`,
  }))

  protected getInitials(name: string): string {
    return name ? name.slice(0, 2).toUpperCase() : '?'
  }

  protected getParticipantColor(name: string): string {
    const colors = [
      'bg-rose-500 text-white',
      'bg-blue-500 text-white',
      'bg-amber-500 text-white',
      'bg-emerald-500 text-white',
      'bg-purple-500 text-white',
      'bg-pink-500 text-white',
      'bg-cyan-500 text-white',
    ]
    let hash = 0
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash)
    }
    return colors[Math.abs(hash) % colors.length]
  }

  protected getSymbol(index: number): string {
    const symbols = ['▲', '◆', '●', '■']
    return symbols[index % symbols.length]
  }

  protected getChoiceColorClass(index: number): string {
    const colors = ['choice-red', 'choice-blue', 'choice-yellow', 'choice-green']
    return colors[index % colors.length]
  }
}
