import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'join/:session-code',
    title: 'Rejoindre une session',
    loadComponent: () => import('./pages/join/join').then(m => m.Join),
  },
]
