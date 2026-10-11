import { HttpClient } from '@angular/common/http'
import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnDestroy, OnInit, signal, WritableSignal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'

import { QRCodeComponent } from 'angularx-qrcode'

import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'

import { SESSION_SERVICE } from '../../services/session'

export type PresenterStage = 'LOBBY' | 'QUESTION' | 'REVEAL' | 'PODIUM'

interface QuestionPresenterModel {
  id: string
  title: string
  explanation?: string
  answers: {
    id: string
    label: string
    isCorrect: boolean
    count?: number
    percentage?: number
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
  id?: string
  index?: number
  label?: string
  value?: string
  pictureFilename?: string
}

interface SseQuestion {
  id?: string
  type?: string
  label?: string
  pictureFilename?: string
  answers?: SseAnswer[]
}

interface SsePayload {
  id?: string
  pictureFilename?: string
  participants?: SseParticipant[]
  timerSeconds?: number
  points?: number
  slideIndex?: number
  totalSlides?: number
  questionId?: string
  title?: string
  label?: string
  answers?: SseAnswer[]
  question?: SseQuestion
  correctAnswerIds?: string[]
  correctAnswerIndices?: number[]
  answeredCount?: number
  totalCount?: number
}

interface SseEnvelope {
  type: string
  payload?: SsePayload
}

@Component({
  selector: 'quizz-session-presenter',
  templateUrl: './session-presenter.html',
  styleUrls: ['./session-presenter.css'],
  imports: [QRCodeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionPresenter implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly config = inject(Config)
  private readonly http = inject(HttpClient)
  private readonly layout = inject(LayoutSettings)

  protected readonly sessionId = signal<string>('')
  protected readonly sessionCode = signal<string>('')
  protected readonly sessionName = signal<string>('')
  protected readonly stage: WritableSignal<PresenterStage> = signal('LOBBY')

  protected readonly podiumStep = signal<number>(0)
  private podiumTimeouts: ReturnType<typeof setTimeout>[] = []

  protected readonly joinUrl = computed(() => {
    const code = this.sessionCode()
    if (!code) return 'https://quezap.fr'
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8000'
    return `${origin}/quizz/join/${code}`
  })

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

  private readonly destroyRef = inject(DestroyRef)
  private eventSource: EventSource | null = null

  ngOnInit(): void {
    this.layout.asWebsite.set(false)
    this.layout.inContainer.set(false)

    const id = this.route.snapshot.paramMap.get('id') || '018e3a2b-8888-7def-8123-abcdef888888'
    this.sessionId.set(id)

    this.sessionService.getSession(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (output.kind === 'success') {
        const session = output.result
        this.sessionCode.set(session.code || session.sessionCode || '')
        this.sessionName.set(session.name || '')
        if (session.status === 'STARTED') {
          this.stage.set('QUESTION')
        }
        else if (session.status === 'ENDED') {
          this.stage.set('PODIUM')
          this.fetchScores()
          this.launchPodiumAnimation()
        }
      }
    })
    this.connectSse(id)
  }

  protected launchPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    this.podiumStep.set(0)
    this.podiumTimeouts.push(
      setTimeout(() => this.podiumStep.set(1), 1200),
      setTimeout(() => this.podiumStep.set(2), 2600),
      setTimeout(() => this.podiumStep.set(3), 4000),
    )
  }

  protected skipPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    this.podiumStep.set(3)
  }

  private clearPodiumTimeouts(): void {
    for (const t of this.podiumTimeouts) {
      clearTimeout(t)
    }
    this.podiumTimeouts = []
  }

  private timerInterval: ReturnType<typeof setInterval> | null = null

  ngOnDestroy(): void {
    this.layout.asWebsite.set(true)
    this.layout.inContainer.set(true)
    this.stopCountdown()
    this.clearPodiumTimeouts()
    if (this.eventSource) {
      this.eventSource.close()
    }
  }

  private startCountdown(seconds: number): void {
    this.stopCountdown()
    this.timer.set(seconds)
    this.timerInterval = setInterval(() => {
      this.timer.update((t) => {
        if (t <= 1) {
          this.stopCountdown()
          return 0
        }
        return t - 1
      })
    }, 1000)
  }

  private stopCountdown(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval)
      this.timerInterval = null
    }
  }

  private connectSse(id: string): void {
    if (typeof window === 'undefined' || typeof EventSource === 'undefined') {
      return
    }
    const apiUrl = this.config.appConfig.value().apiUrl
    this.eventSource = new EventSource(`${apiUrl}/sessions/${id}/events`)

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
      'ANSWER_RECEIVED',
      'SESSION_ENDED',
      'PARTICIPANT_KICKED',
    ]

    for (const name of eventNames) {
      this.eventSource.addEventListener(name, (e: MessageEvent) => {
        parseAndHandle(e.data)
      })
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
          if (payload.slideIndex !== undefined) {
            this.currentQuestionIndex.set(payload.slideIndex - 1)
          }
          if (payload.totalSlides !== undefined) {
            this.totalQuestions.set(payload.totalSlides)
          }
          if (payload.timerSeconds) {
            this.startCountdown(payload.timerSeconds)
          }
          const q = payload.question || payload
          const rawAnswers = q.answers || payload.answers || []
          this.currentQuestion.set({
            id: q.id || payload.questionId || '',
            title: q.label || payload.title || payload.label || '',
            answers: rawAnswers.map((a: SseAnswer, idx: number) => ({
              id: a.id || String(a.index ?? idx),
              label: a.label || a.value || '',
              isCorrect: false,
            })),
          })
        }
        break
      }
      case 'QUESTION_REVEAL': {
        this.stopCountdown()
        this.stage.set('REVEAL')
        const payload = envelope.payload
        if (payload) {
          const correctIds: string[] = payload.correctAnswerIds || []
          const correctIndices: number[] = payload.correctAnswerIndices || []
          const stats = (
            payload as { answersStats?: { answerIndex: number, count: number, percentage: number }[] }
          ).answersStats || []
          const explanation = (payload as { explanation?: string }).explanation

          this.currentQuestion.update(q => ({
            ...q,
            explanation: explanation || q.explanation,
            answers: q.answers.map((a, idx) => {
              const stat = stats.find(s => s.answerIndex === idx)
              return {
                ...a,
                isCorrect: correctIds.includes(a.id) || correctIndices.includes(idx),
                count: stat?.count ?? 0,
                percentage: stat ? Math.round(stat.percentage) : 0,
              }
            }),
          }))
        }
        break
      }
      case 'ANSWER_RECEIVED': {
        const payload = envelope.payload as { answeredCount?: number, totalCount?: number } | undefined
        if (payload && payload.answeredCount !== undefined) {
          this.answersReceived.set(payload.answeredCount)
        }
        break
      }
      case 'SESSION_ENDED':
        this.stopCountdown()
        this.stage.set('PODIUM')
        this.fetchScores()
        this.launchPodiumAnimation()
        break
    }
  }

  private fetchScores(): void {
    this.sessionService.getScores(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (output.kind === 'success') {
        const leaderboard = output.result.leaderboard || []
        this.leaderboard.set(leaderboard.map(s => ({ name: s.participantName || '', score: s.score || 0 })))
      }
    })
  }

  protected startQuiz(): void {
    this.stage.set('QUESTION')
    this.sessionService.startSession(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
  }

  protected revealAnswers(): void {
    this.sessionService.revealAnswers(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
    this.stage.set('REVEAL')
  }

  protected downloadCsv(): void {
    const apiUrl = this.config.appConfig.value().apiUrl
    this.http.get(`${apiUrl}/sessions/${this.sessionId()}/export/csv`, { responseType: 'blob' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((blob) => {
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `session-${this.sessionId()}-resultats.csv`
        a.click()
        window.URL.revokeObjectURL(url)
      })
  }

  protected nextStep(): void {
    this.sessionService.nextQuestion(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
  }

  protected isLastQuestion(): boolean {
    return this.currentQuestionIndex() + 1 >= this.totalQuestions()
  }

  protected kick(name: string): void {
    this.sessionService.kickParticipant(this.sessionId(), name).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
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
