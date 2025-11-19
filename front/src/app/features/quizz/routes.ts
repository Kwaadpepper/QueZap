import { Routes } from '@angular/router'

import { hasActiveSessionGuard } from './guards'

export const routes: Routes = [
  {
    path: 'join',
    title: 'Rejoindre une session',
    loadComponent: () => import('./pages/join/join').then(m => m.Join),
  },
  {
    path: 'join/:session-code',
    title: 'Rejoindre une session',
    loadComponent: () => import('./pages/join/join').then(m => m.Join),
  },
  {
    path: 'expired',
    title: 'Session expirée',
    loadComponent: () => import('./pages/expired/expired').then(m => m.Expired),
  },
  {
    path: 'ended',
    title: 'Quizz terminé',
    loadComponent: () => import('./pages/ended/ended').then(m => m.Ended),
  },
  {
    path: 'lobby',
    title: 'Salle d\'attente',
    canActivate: [
      hasActiveSessionGuard(),
    ],
    loadComponent: () => import('./pages/lobby/lobby').then(m => m.Lobby),
  },
  {
    path: '',
    title: 'Quizz',
    canActivate: [
      hasActiveSessionGuard({ withQuizzRunning: true }),
    ],
    loadComponent: () => import('./pages/quizz-runner/quizz-runner').then(m => m.QuizzRunner),
  },
  {
    path: '**',
    redirectTo: 'expired',
  },
]
