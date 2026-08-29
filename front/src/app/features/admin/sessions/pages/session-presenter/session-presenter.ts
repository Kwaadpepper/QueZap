import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal, WritableSignal,
} from '@angular/core'
import { ActivatedRoute } from '@angular/router'

import { SessionHttpService } from '../../services/session-http'

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

@Component({
  selector: 'quizz-session-presenter',
  templateUrl: './session-presenter.html',
  styleUrls: ['./session-presenter.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessionPresenter implements OnInit {
  private readonly route = inject(ActivatedRoute)
  private readonly sessionHttpService = inject(SessionHttpService)

  protected readonly sessionId = signal<string>('')
  protected readonly sessionCode = signal<string>('QZ-8888')
  protected readonly sessionName = signal<string>('Session en direct')
  protected readonly stage: WritableSignal<PresenterStage> = signal('LOBBY')

  protected readonly participants = signal<{ name: string }[]>([
    { name: 'Lucas' },
    { name: 'Emma' },
    { name: 'Noah' },
    { name: 'Chloé' },
  ])

  protected readonly currentQuestionIndex = signal(0)
  protected readonly totalQuestions = signal(5)
  protected readonly timer = signal(20)
  protected readonly answersReceived = signal(4)

  protected readonly currentQuestion = signal<QuestionPresenterModel>({
    id: 'q-1',
    title: 'Quelle est la capitale de la France ?',
    answers: [
      { id: 'a1', label: 'Lyon', isCorrect: false },
      { id: 'a2', label: 'Marseille', isCorrect: false },
      { id: 'a3', label: 'Paris', isCorrect: true },
      { id: 'a4', label: 'Bordeaux', isCorrect: false },
    ],
  })

  protected readonly leaderboard = signal<LeaderboardItem[]>([
    { name: 'Emma', score: 2850 },
    { name: 'Lucas', score: 2600 },
    { name: 'Noah', score: 2100 },
    { name: 'Chloé', score: 1800 },
  ])

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '018e3a2b-8888-7def-8123-abcdef888888'
    this.sessionId.set(id)
  }

  protected startQuiz(): void {
    this.stage.set('QUESTION')
    this.sessionHttpService.startSession(this.sessionId()).subscribe()
  }

  protected revealAnswers(): void {
    this.stage.set('REVEAL')
  }

  protected nextStep(): void {
    if (this.currentQuestionIndex() + 1 >= this.totalQuestions()) {
      this.stage.set('PODIUM')
    }
    else {
      this.currentQuestionIndex.update(idx => idx + 1)
      this.stage.set('QUESTION')
      this.sessionHttpService.nextQuestion(this.sessionId()).subscribe()
    }
  }

  protected isLastQuestion(): boolean {
    return this.currentQuestionIndex() + 1 >= this.totalQuestions()
  }

  protected kick(name: string): void {
    this.participants.update(list => list.filter(p => p.name !== name))
    this.sessionHttpService.kickParticipant(this.sessionId(), name).subscribe()
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
