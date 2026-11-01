import { HttpClient } from '@angular/common/http'
import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef,
  inject, OnDestroy, OnInit, signal, viewChild, WritableSignal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'

import { QRCodeComponent } from 'angularx-qrcode'
import { Image } from 'primeng/image'

import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'

import {
  QuestionIcon,
  QuestionIconType,
} from '../../../../quizz/components/question-view/parts/question-icon/question-icon'
import { SESSION_SERVICE } from '../../services/session'

export type PresenterStage = 'LOBBY' | 'QUESTION' | 'REVEAL' | 'LEADERBOARD' | 'PODIUM'

export interface IntermediateLeaderboardEntry {
  rank: number
  participantName: string
  score: number
  delta: number
  streak: number
}

interface QuestionPresenterModel {
  id: string
  title: string
  type?: string
  pictureUrl?: string
  explanation?: string
  acceptedAnswers?: string[]
  answers: {
    id: string
    label: string
    pictureFilename?: string
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
  pictureUrl?: string
  pictureFilename?: string
}

interface SseQuestion {
  id?: string
  type?: string
  label?: string
  pictureUrl?: string
  pictureFilename?: string
  answers?: SseAnswer[]
}

interface SsePayload {
  id?: string
  pictureUrl?: string
  pictureFilename?: string
  participants?: SseParticipant[]
  timerSeconds?: number
  deadline?: string
  remainingSeconds?: number
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
  acceptedAnswers?: string[]
  answeredCount?: number
  totalCount?: number
  leaderboard?: IntermediateLeaderboardEntry[]
}

interface SseEnvelope {
  type: string
  payload?: SsePayload
}

@Component({
  selector: 'quizz-session-presenter',
  templateUrl: './session-presenter.html',
  styleUrls: ['./session-presenter.css'],
  imports: [QRCodeComponent, QuestionIcon, Image],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionPresenter implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly config = inject(Config)
  private readonly http = inject(HttpClient)
  private readonly layout = inject(LayoutSettings)

  protected readonly autoReveal = signal<boolean>(true)
  protected readonly intermediateLeaderboard = signal<IntermediateLeaderboardEntry[]>([])

  protected readonly progressionMode = signal<'MANUAL' | 'AUTOMATIC'>('MANUAL')
  protected readonly explanationDurationSeconds = signal<number>(15)
  protected readonly isPaused = signal<boolean>(false)
  protected readonly explanationTimer = signal<number>(15)
  protected readonly classMetrics = signal<{
    totalParticipants: number
    averageScore: number
    globalSuccessRate: number
  } | null>(null)

  protected readonly top5Entries = computed(() => this.leaderboard().slice(0, 5))

  protected toggleAutoReveal(): void {
    this.autoReveal.update(v => !v)
  }

  protected toggleProgressionMode(): void {
    const nextMode = this.progressionMode() === 'MANUAL' ? 'AUTOMATIC' : 'MANUAL'
    this.setProgressionMode(nextMode)
  }

  protected setProgressionMode(mode: 'MANUAL' | 'AUTOMATIC'): void {
    this.progressionMode.set(mode)
    this.sessionService.updateSettings(this.sessionId(), {
      progressionMode: mode,
      explanationDurationSeconds: this.explanationDurationSeconds(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()

    if (
      mode === 'AUTOMATIC'
      && (this.stage() === 'REVEAL' || this.stage() === 'LEADERBOARD')
      && !this.explanationTimerInterval
    ) {
      this.startExplanationCountdown()
    }
    else if (mode === 'MANUAL') {
      this.stopExplanationCountdown()
    }
  }

  protected setExplanationDuration(seconds: number): void {
    this.explanationDurationSeconds.set(seconds)
    this.sessionService.updateSettings(this.sessionId(), {
      progressionMode: this.progressionMode(),
      explanationDurationSeconds: seconds,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
  }

  protected togglePause(): void {
    const nextPaused = !this.isPaused()
    this.isPaused.set(nextPaused)
    this.sessionService.pauseSession(this.sessionId(), nextPaused)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe()
  }

  protected showIntermediateLeaderboard(): void {
    this.stage.set('LEADERBOARD')
  }

  protected readonly sessionId = signal<string>('')
  protected readonly sessionCode = signal<string>('')
  protected readonly sessionName = signal<string>('')
  protected readonly stage: WritableSignal<PresenterStage> = signal('LOBBY')

  protected readonly confettiCanvas = viewChild<ElementRef<HTMLCanvasElement>>('confettiCanvas')
  private confettiAnimId: number | null = null

  protected readonly podiumStep = signal<number>(0)
  private podiumTimeouts: ReturnType<typeof setTimeout>[] = []

  protected readonly displayJoinUrl = computed(() => {
    const host = typeof window !== 'undefined' ? window.location.host : 'localhost:8000'
    return `${host}/quizz/join`
  })

  protected readonly joinUrl = computed(() => {
    const code = this.sessionCode()
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:8000'
    if (!code) return `${origin}/quizz/join`
    return `${origin}/quizz/join/${code}`
  })

  protected readonly participants = signal<{ name: string }[]>([])

  protected readonly currentQuestionIndex = signal(0)
  protected readonly totalQuestions = signal(1)
  protected readonly timer = signal(30)
  protected readonly initialQuestionDuration = signal(30)

  protected readonly timerProgressPercent = computed(() => {
    const total = this.initialQuestionDuration() || 30
    return Math.max(0, Math.min(100, (this.timer() / total) * 100))
  })

  protected readonly answersReceived = signal(0)

  protected readonly currentQuestion = signal<QuestionPresenterModel>({
    id: '',
    title: '',
    answers: [],
  })

  protected readonly leaderboard = signal<LeaderboardItem[]>([])

  protected readonly liveAnnouncement = computed<string>(() => {
    switch (this.stage()) {
      case 'LOBBY':
        return `Salon d'attente. ${this.participants().length} participants connectés.`
      case 'QUESTION':
        return (
          `Question ${this.currentQuestionIndex() + 1} sur ${this.totalQuestions()} : `
          + `${this.currentQuestion().title}`
        )
      case 'REVEAL':
        return 'Bonne réponse révélée.'
      case 'LEADERBOARD':
        return 'Classement intermédiaire affiché.'
      case 'PODIUM':
        return 'Podium final de la classe.'
      default:
        return ''
    }
  })

  private readonly destroyRef = inject(DestroyRef)
  private eventSource: EventSource | null = null
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0

  protected readonly qrZoomed = signal(false)

  protected toggleQrZoom(): void {
    this.qrZoomed.update(v => !v)
  }

  ngOnInit(): void {
    this.layout.asWebsite.set(false)
    this.layout.inContainer.set(false)

    const id = this.route.snapshot.paramMap.get('id') || '018e3a2b-8888-7def-8123-abcdef888888'
    this.sessionId.set(id)

    const isUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(id)

    this.sessionService.getSession(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (output.kind === 'success') {
        const session = output.result
        this.sessionId.set(session.id || '')
        this.sessionCode.set(session.code || '')
        this.sessionName.set(session.name || '')
        if (session.participants && session.participants.length > 0) {
          this.participants.set(session.participants.map(p => ({ name: p.name || '' })))
        }
        if (!isUuid && session.id) {
          if (this.eventSource) {
            this.eventSource.close()
          }
          this.connectSse(session.id)
        }
        if (session.progressionMode === 'AUTOMATIC' || session.progressionMode === 'MANUAL') {
          this.progressionMode.set(session.progressionMode)
        }
        if (session.explanationDurationSeconds) {
          this.explanationDurationSeconds.set(session.explanationDurationSeconds)
        }
        if (session.isPaused !== undefined) {
          this.isPaused.set(session.isPaused)
        }
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
    if (isUuid) {
      this.connectSse(id)
    }
  }

  protected launchPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    this.podiumStep.set(0)
    this.podiumTimeouts.push(
      setTimeout(() => this.podiumStep.set(1), 1200),
      setTimeout(() => this.podiumStep.set(2), 2600),
      setTimeout(() => {
        this.podiumStep.set(3)
        this.triggerCanvasConfetti()
      }, 4000),
    )
  }

  protected skipPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    this.podiumStep.set(3)
    this.triggerCanvasConfetti()
  }

  private triggerCanvasConfetti(): void {
    if (typeof window === 'undefined') return
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      return
    }
    setTimeout(() => {
      const canvasEl = this.confettiCanvas()?.nativeElement
      if (canvasEl) {
        this.startCanvasConfetti(canvasEl)
      }
    }, 100)
  }

  private startCanvasConfetti(canvas: HTMLCanvasElement): void {
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight

    const particles: {
      x: number
      y: number
      size: number
      color: string
      vx: number
      vy: number
      angle: number
      dAngle: number
    }[] = []

    const colors = ['#ef4444', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#fbbf24']
    for (let i = 0; i < 80; i++) {
      particles.push({
        x: Math.random() * canvas.width,
        y: -20 - Math.random() * 200,
        size: 8 + Math.random() * 8,
        color: colors[Math.floor(Math.random() * colors.length)],
        vx: (Math.random() - 0.5) * 4,
        vy: 2.5 + Math.random() * 4,
        angle: Math.random() * Math.PI * 2,
        dAngle: (Math.random() - 0.5) * 0.1,
      })
    }

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      for (const p of particles) {
        p.x += p.vx
        p.y += p.vy
        p.angle += p.dAngle

        ctx.save()
        ctx.translate(p.x, p.y)
        ctx.rotate(p.angle)
        ctx.fillStyle = p.color
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 1.4)
        ctx.restore()

        if (p.y > canvas.height + 30) {
          p.y = -20
          p.x = Math.random() * canvas.width
        }
      }
      this.confettiAnimId = requestAnimationFrame(draw)
    }
    this.confettiAnimId = requestAnimationFrame(draw)
  }

  private clearPodiumTimeouts(): void {
    if (this.confettiAnimId !== null && typeof cancelAnimationFrame !== 'undefined') {
      cancelAnimationFrame(this.confettiAnimId)
      this.confettiAnimId = null
    }
    for (const t of this.podiumTimeouts) {
      clearTimeout(t)
    }
    this.podiumTimeouts = []
  }

  private timerInterval: ReturnType<typeof setInterval> | null = null
  private zeroRevealTimeout: ReturnType<typeof setTimeout> | null = null
  private explanationTimerInterval: ReturnType<typeof setInterval> | null = null

  ngOnDestroy(): void {
    this.layout.asWebsite.set(true)
    this.layout.inContainer.set(true)
    this.stopCountdown()
    this.stopExplanationCountdown()
    this.clearPodiumTimeouts()
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }

  private startCountdown(seconds: number): void {
    this.stopCountdown()
    this.initialQuestionDuration.set(seconds)
    this.timer.set(seconds)

    if (seconds <= 0) {
      if (this.autoReveal() && this.stage() === 'QUESTION') {
        this.zeroRevealTimeout = setTimeout(() => {
          this.zeroRevealTimeout = null
          if (this.autoReveal() && this.stage() === 'QUESTION' && !this.isPaused()) {
            this.revealAnswers()
          }
        }, 250)
      }
      return
    }

    this.timerInterval = setInterval(() => {
      if (this.isPaused()) {
        return
      }
      const current = this.timer()
      if (current <= 1) {
        this.timer.set(0)
        this.stopCountdown()
        if (this.autoReveal() && this.stage() === 'QUESTION') {
          this.revealAnswers()
        }
      }
      else {
        this.timer.set(current - 1)
      }
    }, 1000)
  }

  private stopCountdown(): void {
    if (this.zeroRevealTimeout) {
      clearTimeout(this.zeroRevealTimeout)
      this.zeroRevealTimeout = null
    }
    if (this.timerInterval) {
      clearInterval(this.timerInterval)
      this.timerInterval = null
    }
  }

  private startExplanationCountdown(): void {
    this.stopExplanationCountdown()
    const duration = this.explanationDurationSeconds()
    this.explanationTimer.set(duration)

    if (this.progressionMode() !== 'AUTOMATIC') {
      return
    }

    this.explanationTimerInterval = setInterval(() => {
      if (this.isPaused()) {
        return
      }
      const current = this.explanationTimer()
      if (current <= 1) {
        this.explanationTimer.set(0)
        this.stopExplanationCountdown()
        if (this.stage() === 'REVEAL' || this.stage() === 'LEADERBOARD') {
          this.nextStep()
        }
      }
      else {
        this.explanationTimer.set(current - 1)
      }
    }, 1000)
  }

  private stopExplanationCountdown(): void {
    if (this.explanationTimerInterval) {
      clearInterval(this.explanationTimerInterval)
      this.explanationTimerInterval = null
    }
  }

  private currentConnectedSessionId = ''

  private connectSse(id: string): void {
    if (typeof window === 'undefined' || typeof EventSource === 'undefined') {
      return
    }
    if (
      this.eventSource
      && this.currentConnectedSessionId === id
      && this.eventSource.readyState !== EventSource.CLOSED
    ) {
      return
    }
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    this.currentConnectedSessionId = id

    const apiUrl = this.config.appConfig.value().apiUrl
    this.eventSource = new EventSource(`${apiUrl}/sessions/${id}/events?ngsw-bypass=true`)

    const parseAndHandle = (data: string) => {
      try {
        const envelope: SseEnvelope = JSON.parse(data)
        this.handleEvent(envelope)
      }
      catch (e) {
        console.debug('Failed to parse SSE payload', e)
      }
    }

    this.eventSource.onopen = () => {
      this.reconnectAttempts = 0
    }

    this.eventSource.onerror = (err) => {
      console.warn('SSE connection error, scheduling reconnect...', err)
      if (this.eventSource?.readyState === EventSource.CLOSED) {
        if (this.reconnectTimeout) {
          clearTimeout(this.reconnectTimeout)
        }
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 15000)
        this.reconnectAttempts++
        this.reconnectTimeout = setTimeout(() => {
          this.connectSse(id)
        }, delay)
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
      'SESSION_PAUSE_STATUS',
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
          this.stopExplanationCountdown()
          this.isPaused.set(false)
          this.stage.set('QUESTION')
          if (payload.answeredCount !== undefined) {
            this.answersReceived.set(payload.answeredCount)
          }
          else {
            this.answersReceived.set(0)
          }
          if (payload.slideIndex !== undefined) {
            this.currentQuestionIndex.set(payload.slideIndex - 1)
          }
          if (payload.totalSlides !== undefined) {
            this.totalQuestions.set(payload.totalSlides)
          }
          const q = payload.question || payload
          const qType = (q as { type?: string }).type || (payload as { type?: string }).type || ''
          const rawAnswers = q.answers || payload.answers || []
          const picture
            = q.pictureUrl
              || q.pictureFilename
              || payload.pictureUrl
              || payload.pictureFilename
              || (q as { picture?: string }).picture
          let mappedAnswers = rawAnswers.map((a: SseAnswer, idx: number) => ({
            id: a.id || String(a.index ?? idx),
            label: a.label || a.value || '',
            pictureFilename: a.pictureUrl || a.pictureFilename || (a as { picture?: string }).picture,
            isCorrect: false,
          }))
          if (qType === 'BOOLEAN' && mappedAnswers.length <= 1) {
            mappedAnswers = [
              { id: '0', label: 'Vrai', pictureFilename: undefined, isCorrect: false },
              { id: '1', label: 'Faux', pictureFilename: undefined, isCorrect: false },
            ]
          }
          this.currentQuestion.set({
            id: q.id || payload.questionId || '',
            title: q.label || payload.title || payload.label || '',
            type: qType,
            pictureUrl: picture,
            answers: mappedAnswers,
          })
          const seconds = payload.remainingSeconds ?? payload.timerSeconds ?? 30
          const totalDuration = payload.timerSeconds ?? seconds
          this.startCountdown(seconds)
          this.initialQuestionDuration.set(totalDuration)
        }
        break
      }
      case 'QUESTION_REVEAL': {
        this.stopCountdown()
        this.stage.set('REVEAL')
        this.startExplanationCountdown()
        const payload = envelope.payload
        if (payload) {
          const correctIds: string[] = payload.correctAnswerIds || []
          const correctIndices: number[] = payload.correctAnswerIndices || []
          const stats = (
            payload as { answersStats?: { answerIndex: number, count: number, percentage: number }[] }
          ).answersStats || []
          const explanation = (payload as { explanation?: string }).explanation
          const acceptedAnswers: string[] = payload.acceptedAnswers || []

          this.currentQuestion.update((q) => {
            const updatedAnswers = (q.type === 'CASH' && acceptedAnswers.length > 0)
              ? acceptedAnswers.map((label, idx) => {
                  const stat = stats.find(s => s.answerIndex === idx)
                  return {
                    id: String(idx),
                    label,
                    isCorrect: true,
                    count: stat?.count ?? 0,
                    percentage: stat ? Math.round(stat.percentage) : 0,
                  }
                })
              : q.answers.map((a, idx) => {
                  const stat = stats.find(s => s.answerIndex === idx)
                  return {
                    ...a,
                    isCorrect: correctIds.includes(a.id) || correctIndices.includes(idx),
                    count: stat?.count ?? 0,
                    percentage: stat ? Math.round(stat.percentage) : 0,
                  }
                })

            return {
              ...q,
              explanation: explanation || q.explanation,
              acceptedAnswers: acceptedAnswers.length > 0 ? acceptedAnswers : q.acceptedAnswers,
              answers: updatedAnswers,
            }
          })
        }
        break
      }
      case 'INTERMEDIATE_LEADERBOARD': {
        const payload = envelope.payload as {
          slideIndex?: number
          leaderboard?: IntermediateLeaderboardEntry[]
        } | undefined
        if (payload?.leaderboard) {
          this.intermediateLeaderboard.set(payload.leaderboard)
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
      case 'PARTICIPANT_KICKED': {
        const payload = envelope.payload as { participantName?: string } | undefined
        if (payload?.participantName) {
          const kickedName = payload.participantName.trim().toLowerCase()
          this.participants.update(list => list.filter(p => p.name.trim().toLowerCase() !== kickedName))
        }
        break
      }
      case 'SESSION_PAUSE_STATUS': {
        const payload = envelope.payload as { isPaused?: boolean } | undefined
        if (payload?.isPaused !== undefined) {
          this.isPaused.set(payload.isPaused)
        }
        break
      }
      case 'SESSION_ENDED': {
        this.stopCountdown()
        this.stopExplanationCountdown()
        this.isPaused.set(false)
        this.stage.set('PODIUM')
        const payload = envelope.payload as {
          finalLeaderboard?: { participantName: string, score: number }[]
          metrics?: { totalParticipants: number, averageScore: number, globalSuccessRate: number }
        } | undefined
        if (payload?.metrics) {
          this.classMetrics.set(payload.metrics)
        }
        if (payload?.finalLeaderboard && payload.finalLeaderboard.length > 0) {
          const seen = new Set<string>()
          const cleanLeaderboard: LeaderboardItem[] = []
          for (const item of payload.finalLeaderboard) {
            const key = (item.participantName || '').trim().toLowerCase()
            if (key && !seen.has(key)) {
              seen.add(key)
              cleanLeaderboard.push({ name: item.participantName || '', score: item.score || 0 })
            }
          }
          this.leaderboard.set(cleanLeaderboard)
        }
        this.fetchScores()
        this.launchPodiumAnimation()
        break
      }
    }
  }

  private fetchScores(): void {
    this.sessionService.getScores(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (output.kind === 'success') {
        const leaderboard = output.result.leaderboard || []
        const seen = new Set<string>()
        const cleanLeaderboard: LeaderboardItem[] = []
        for (const item of leaderboard) {
          const key = (item.participantName || '').trim().toLowerCase()
          if (key && !seen.has(key)) {
            seen.add(key)
            cleanLeaderboard.push({ name: item.participantName || '', score: item.score || 0 })
          }
        }
        this.leaderboard.set(cleanLeaderboard)
        if (!this.classMetrics() && cleanLeaderboard.length > 0) {
          const totalScore = cleanLeaderboard.reduce((acc, cur) => acc + (cur.score || 0), 0)
          const avg = Math.round(totalScore / cleanLeaderboard.length)
          this.classMetrics.set({
            totalParticipants: cleanLeaderboard.length,
            averageScore: avg,
            globalSuccessRate: 0,
          })
        }
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
    this.stopExplanationCountdown()
    this.sessionService.nextQuestion(this.sessionId()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe()
  }

  protected isLastQuestion(): boolean {
    return this.currentQuestionIndex() + 1 >= this.totalQuestions()
  }

  protected readonly playerToKick = signal<string | null>(null)

  protected requestKick(name: string): void {
    this.playerToKick.set(name)
  }

  protected cancelKick(): void {
    this.playerToKick.set(null)
  }

  protected confirmKick(): void {
    const name = this.playerToKick()
    if (name) {
      this.kick(name)
      this.playerToKick.set(null)
    }
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

  protected getChoiceForm(index: number): QuestionIconType {
    const isBool
      = this.currentQuestion().type === 'BOOLEAN'
        || (this.currentQuestion().answers.length === 2
          && this.currentQuestion().answers[0].label === 'Vrai')
    if (isBool) {
      return index === 0 ? QuestionIconType.Circle : QuestionIconType.Cross
    }
    const forms = [
      QuestionIconType.Circle,
      QuestionIconType.Diamond,
      QuestionIconType.Cross,
      QuestionIconType.Hexagon,
    ]
    return forms[index % forms.length]
  }

  protected getSymbol(index: number): string {
    return String(index + 1)
  }

  protected getChoiceColorClass(index: number): string {
    const isBool
      = this.currentQuestion().type === 'BOOLEAN'
        || (this.currentQuestion().answers.length === 2
          && this.currentQuestion().answers[0].label === 'Vrai')
    if (isBool) {
      return index === 0 ? 'choice-green' : 'choice-red'
    }
    const colors = ['choice-red', 'choice-blue', 'choice-yellow', 'choice-green']
    return colors[index % colors.length]
  }
}
