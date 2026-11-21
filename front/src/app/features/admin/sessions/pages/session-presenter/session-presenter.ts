import { NgClass } from '@angular/common'
import {
  ChangeDetectionStrategy, Component, computed, DestroyRef, ElementRef,
  inject, OnDestroy, OnInit, signal, viewChild, WritableSignal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'

import { QRCodeComponent } from 'angularx-qrcode'
import { MessageService } from 'primeng/api'
import { Image } from 'primeng/image'

import { ServiceError, UnauthorizedError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'
import { isFailure, isSuccess } from '@quezap/core/types'

import {
  QuestionIcon,
  QuestionIconType,
} from '../../../../quizz/components/question-view/parts/question-icon/question-icon'
import { SESSION_SERVICE } from '../../services/session'

export type PresenterStage = 'LOBBY' | 'SYNCING' | 'QUESTION' | 'REVEAL' | 'LEADERBOARD' | 'PODIUM'

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
  imports: [NgClass, RouterLink, QRCodeComponent, QuestionIcon, Image],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionPresenter implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute)
  private readonly router = inject(Router)
  private readonly sessionService = inject(SESSION_SERVICE)
  private readonly config = inject(Config)
  private readonly messageService = inject(MessageService, { optional: true })
  private readonly layout = inject(LayoutSettings)

  protected readonly autoReveal = signal<boolean>(true)
  protected readonly intermediateLeaderboard = signal<IntermediateLeaderboardEntry[]>([])

  protected readonly progressionMode = signal<'MANUAL' | 'AUTOMATIC'>('MANUAL')
  protected readonly explanationDurationSeconds = signal<number>(15)
  protected readonly explanationDurationOptions = [5, 10, 15, 20, 30]
  protected readonly mobileCorrectionEnabled = signal<boolean>(false)
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
    const previousMode = this.progressionMode()
    this.progressionMode.set(mode)
    this.sessionService.updateSettings(this.sessionId(), {
      progressionMode: mode,
      explanationDurationSeconds: this.explanationDurationSeconds(),
      mobileCorrectionEnabled: this.mobileCorrectionEnabled(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (isFailure(output)) {
        this.progressionMode.set(previousMode)
        if (previousMode === 'MANUAL') {
          this.stopExplanationCountdown()
        }
        this.handleCommandFailure(output.error, 'Impossible de modifier le mode de progression')
      }
    })

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

  protected onExplanationDurationChange(event: Event): void {
    const target = event.target as HTMLSelectElement | null
    const value = Number(target?.value)
    if (!Number.isNaN(value) && value > 0) {
      this.setExplanationDuration(value)
    }
  }

  protected setExplanationDuration(seconds: number): void {
    const previousDuration = this.explanationDurationSeconds()
    this.explanationDurationSeconds.set(seconds)
    this.sessionService.updateSettings(this.sessionId(), {
      progressionMode: this.progressionMode(),
      explanationDurationSeconds: seconds,
      mobileCorrectionEnabled: this.mobileCorrectionEnabled(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (isFailure(output)) {
        this.explanationDurationSeconds.set(previousDuration)
        this.handleCommandFailure(output.error, 'Impossible de modifier la durée d’explication')
      }
    })
  }

  protected toggleMobileCorrection(): void {
    const previous = this.mobileCorrectionEnabled()
    const next = !previous
    this.mobileCorrectionEnabled.set(next)
    this.sessionService.updateSettings(this.sessionId(), {
      progressionMode: this.progressionMode(),
      explanationDurationSeconds: this.explanationDurationSeconds(),
      mobileCorrectionEnabled: next,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (isFailure(output)) {
        this.mobileCorrectionEnabled.set(previous)
        this.handleCommandFailure(output.error, 'Impossible de modifier la correction mobile')
      }
    })
  }

  protected togglePause(): void {
    const previousPaused = this.isPaused()
    const nextPaused = !previousPaused
    this.isPaused.set(nextPaused)
    this.syncPauseTiming(nextPaused)
    this.sessionService.pauseSession(this.sessionId(), nextPaused)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.isPaused.set(previousPaused)
          this.syncPauseTiming(previousPaused)
          this.handleCommandFailure(output.error, 'Impossible de modifier l’état de pause')
        }
        else if (isSuccess(output) && output.result.isPaused !== undefined) {
          this.isPaused.set(output.result.isPaused)
          this.syncPauseTiming(output.result.isPaused)
        }
      })
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
      case 'SYNCING':
        return 'Synchronisation de la session en cours.'
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
  private hasReceivedInitialQuestionEvent = false

  protected readonly qrZoomed = signal(false)

  protected toggleQrZoom(): void {
    this.qrZoomed.update(v => !v)
  }

  private handleCommandFailure(error: ServiceError, fallbackDetail: string): void {
    const detail = error.message || fallbackDetail
    this.messageService?.add({
      severity: 'error',
      summary: 'Erreur',
      detail,
    })
    if (error instanceof UnauthorizedError) {
      this.router.navigate(['/auth/login'])
    }
  }

  private syncPauseTiming(paused: boolean): void {
    if (paused) {
      if (this.pausedAtMs === null) {
        this.pausedAtMs = Date.now()
      }
    }
    else if (this.pausedAtMs !== null) {
      if (this.questionDeadlineMs !== null) {
        this.questionDeadlineMs += Date.now() - this.pausedAtMs
      }
      this.pausedAtMs = null
    }
  }

  ngOnInit(): void {
    this.layout.asWebsite.set(false)
    this.layout.inContainer.set(false)

    const id = this.route.snapshot.paramMap.get('id') || '018e3a2b-8888-7def-8123-abcdef888888'
    this.sessionId.set(id)

    const isUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(id)

    this.sessionService.getSession(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((output) => {
      if (isFailure(output)) {
        this.handleCommandFailure(output.error, 'Impossible de charger la session')
        return
      }
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
      if (session.mobileCorrectionEnabled !== undefined) {
        this.mobileCorrectionEnabled.set(session.mobileCorrectionEnabled)
      }
      if (session.isPaused !== undefined) {
        this.isPaused.set(session.isPaused)
        this.syncPauseTiming(session.isPaused)
      }
      const isStartedAndNotEnded = session.status === 'STARTED'
        || (session.startedAt != null && session.endedAt == null)
      if (session.status === 'ENDED' || session.endedAt != null) {
        this.stage.set('PODIUM')
        this.fetchScores()
        this.podiumStep.set(3)
      }
      else if (isStartedAndNotEnded && !this.hasReceivedInitialQuestionEvent) {
        this.stage.set('SYNCING')
      }
    })
    if (isUuid) {
      this.connectSse(id)
    }
  }

  protected launchPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    const count = this.leaderboard().length
    if (count === 0) {
      this.podiumStep.set(3)
      return
    }
    if (count === 1) {
      this.podiumStep.set(2)
      this.podiumTimeouts.push(
        setTimeout(() => {
          this.podiumStep.set(3)
          this.triggerCanvasConfetti()
        }, 650),
      )
      return
    }
    if (count === 2) {
      this.podiumStep.set(1)
      this.podiumTimeouts.push(
        setTimeout(() => this.podiumStep.set(2), 500),
        setTimeout(() => {
          this.podiumStep.set(3)
          this.triggerCanvasConfetti()
        }, 1250),
      )
      return
    }
    this.podiumStep.set(0)
    this.podiumTimeouts.push(
      setTimeout(() => this.podiumStep.set(1), 450),
      setTimeout(() => this.podiumStep.set(2), 1150),
      setTimeout(() => {
        this.podiumStep.set(3)
        this.triggerCanvasConfetti()
      }, 1950),
    )
  }

  protected skipPodiumAnimation(): void {
    this.clearPodiumTimeouts()
    this.podiumStep.set(3)
    if (this.leaderboard().length > 0) {
      this.triggerCanvasConfetti()
    }
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
    }, 80)
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
    for (let i = 0; i < 65; i++) {
      particles.push({
        x: Math.random() * canvas.width,
        y: -20 - Math.random() * 180,
        size: 7 + Math.random() * 6,
        color: colors[Math.floor(Math.random() * colors.length)],
        vx: (Math.random() - 0.5) * 3.5,
        vy: 2.8 + Math.random() * 3.8,
        angle: Math.random() * Math.PI * 2,
        dAngle: (Math.random() - 0.5) * 0.12,
      })
    }

    const startTime = performance.now()
    const totalDurationMs = 4200
    const fadeStartMs = 2800

    const draw = (now: number) => {
      const elapsed = now - startTime
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      if (elapsed >= totalDurationMs) {
        this.confettiAnimId = null
        return
      }

      const alpha = elapsed > fadeStartMs
        ? Math.max(0, 1 - (elapsed - fadeStartMs) / (totalDurationMs - fadeStartMs))
        : 1
      ctx.globalAlpha = alpha

      for (const p of particles) {
        p.x += p.vx
        p.y += p.vy
        p.angle += p.dAngle

        ctx.save()
        ctx.translate(p.x, p.y)
        ctx.rotate(p.angle)
        ctx.fillStyle = p.color
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 1.35)
        ctx.restore()

        if (p.y > canvas.height + 24 && elapsed < fadeStartMs) {
          p.y = -16
          p.x = Math.random() * canvas.width
        }
      }
      ctx.globalAlpha = 1
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
  private questionDeadlineMs: number | null = null
  private pausedAtMs: number | null = null

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

  private startCountdown(seconds: number, deadline?: string): void {
    this.stopCountdown()

    const parsedDeadline = deadline ? Date.parse(deadline) : Number.NaN
    const effectiveSeconds = !Number.isNaN(parsedDeadline)
      ? Math.max(0, Math.ceil((parsedDeadline - Date.now()) / 1000))
      : Math.max(0, seconds)

    this.questionDeadlineMs = !Number.isNaN(parsedDeadline)
      ? parsedDeadline
      : Date.now() + effectiveSeconds * 1000
    this.pausedAtMs = this.isPaused() ? Date.now() : null

    this.initialQuestionDuration.set(effectiveSeconds)
    this.timer.set(effectiveSeconds)

    if (effectiveSeconds <= 0) {
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
      if (this.isPaused() || this.questionDeadlineMs === null) {
        return
      }
      const remaining = Math.max(0, Math.ceil((this.questionDeadlineMs - Date.now()) / 1000))
      this.timer.set(remaining)
      if (remaining <= 0) {
        this.stopCountdown()
        if (this.autoReveal() && this.stage() === 'QUESTION') {
          this.revealAnswers()
        }
      }
    }, 250)
  }

  private stopCountdown(): void {
    this.questionDeadlineMs = null
    this.pausedAtMs = null
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
        this.hasReceivedInitialQuestionEvent = true
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
          this.startCountdown(seconds, payload.deadline)
          this.initialQuestionDuration.set(totalDuration)
        }
        break
      }
      case 'QUESTION_REVEAL': {
        this.hasReceivedInitialQuestionEvent = true
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
          this.syncPauseTiming(payload.isPaused)
        }
        break
      }
      case 'SESSION_ENDED': {
        this.hasReceivedInitialQuestionEvent = true
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
      if (isFailure(output)) {
        this.handleCommandFailure(output.error, 'Impossible de récupérer les scores')
        return
      }
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
    })
  }

  protected startQuiz(): void {
    const previousStage = this.stage()
    this.stage.set('QUESTION')
    this.sessionService.startSession(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.stage.set(previousStage)
          this.handleCommandFailure(output.error, 'Impossible de démarrer la session')
        }
      })
  }

  protected revealAnswers(): void {
    const previousStage = this.stage()
    this.stage.set('REVEAL')
    this.sessionService.revealAnswers(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.stage.set(previousStage)
          this.handleCommandFailure(output.error, 'Impossible de révéler les réponses')
        }
      })
  }

  protected downloadCsv(): void {
    this.sessionService.exportCsv(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.handleCommandFailure(output.error, 'Échec du téléchargement du fichier CSV')
          return
        }
        const url = window.URL.createObjectURL(output.result)
        const a = document.createElement('a')
        a.href = url
        a.download = `session-${this.sessionId()}-resultats.csv`
        a.click()
        window.URL.revokeObjectURL(url)
      })
  }

  protected nextStep(): void {
    this.stopExplanationCountdown()
    this.sessionService.nextQuestion(this.sessionId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.handleCommandFailure(output.error, 'Impossible de passer à la question suivante')
        }
      })
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
    this.sessionService.kickParticipant(this.sessionId(), name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((output) => {
        if (isFailure(output)) {
          this.handleCommandFailure(output.error, 'Impossible d’exclure le participant')
        }
      })
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
