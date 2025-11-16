import { Routes } from '@angular/router'

export const routes: Routes = [
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
]
