import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'login',
    title: 'Connexion',
    loadComponent: () => import('./pages/login/login').then(m => m.Login),
  },
  {
    path: 'reset',
    title: 'Mot de passe oublié',
    loadComponent: () => import('./pages/forgotten-password/forgotten-password').then(m => m.ForgottenPassword),
  },
]
