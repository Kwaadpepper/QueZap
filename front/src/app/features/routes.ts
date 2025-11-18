import { Routes } from '@angular/router'

import { isAuthenticatedGuard, isUnauthenticatedGuard } from '@quezap/core/guards'

import { ActiveSessionPersistence, SESSION_API_SERVICE, SESSION_OBSERVER_SERVICE, SessionApiMockService, SessionObserverMockService } from './quizz/services'
import { ActiveSessionStore } from './quizz/stores'

export const routes: Routes = [
  {
    path: 'admin',
    title: 'Administration',
    loadChildren: () => import('./admin/routes').then(m => m.routes),
    canActivateChild: [
      isAuthenticatedGuard,
    ],
  },
  {
    path: 'auth',
    title: 'Authentification',
    loadChildren: () => import('./auth/routes').then(m => m.routes),
    canActivateChild: [
      isUnauthenticatedGuard,
    ],
  },
  {
    path: 'activation',
    title: 'Activation du compte',
    loadChildren: () => import('./activation/routes').then(m => m.routes),
    canActivateChild: [
      isUnauthenticatedGuard,
    ],
  },
  {
    path: 'quizz',
    title: 'Quizz',
    providers: [
      ActiveSessionStore,
      ActiveSessionPersistence,
      { provide: SESSION_API_SERVICE, useClass: SessionApiMockService },
      { provide: SESSION_OBSERVER_SERVICE, useClass: SessionObserverMockService },
    ],
    loadChildren: () => import('./quizz/routes').then(m => m.routes),
  },
]
