import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion',
    loadComponent: () => import('./pages/login/login').then(m => m.Login),
  },
]
