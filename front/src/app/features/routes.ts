import { inject } from '@angular/core'
import { Routes } from '@angular/router'

import { AuthenticatedGuard } from '@quezap/core/guards'
import { UnAuthenticatedGuard } from '@quezap/core/guards/auth/unauthenticated-guard'

export const routes: Routes = [
  {
    path: 'admin',
    title: 'Administration',
    loadChildren: () => import('./admin/routes').then(m => m.routes),
    canActivateChild: [
      () => inject(AuthenticatedGuard).canActivateChild(),
    ],
  },
  {
    path: 'auth',
    title: 'Authentification',
    loadChildren: () => import('./auth/routes').then(m => m.routes),
    canActivateChild: [
      () => inject(UnAuthenticatedGuard).canActivateChild(),
    ],
  },
  {
    path: 'activation',
    title: 'Activation du compte',
    loadChildren: () => import('./activation/routes').then(m => m.routes),
    canActivateChild: [
      () => inject(UnAuthenticatedGuard).canActivateChild(),
    ],
  },
  {
    path: 'quizz',
    title: 'Quizz',
    loadChildren: () => import('./quizz/routes').then(m => m.routes),
  },
]
