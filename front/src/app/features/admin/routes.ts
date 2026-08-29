import { Routes } from '@angular/router'

import { QuezapPageStore } from './quezaps/stores'
import { SessionPageStore } from './sessions/stores'
import { ThemePageStore } from './themes/stores'

export const routes: Routes = [
  {
    path: 'dashboard',
    title: 'Tableau de bord',
    loadChildren: () => import('./dashboard/routes').then(m => m.routes),
  },
  {
    path: 'sessions',
    title: 'Sessions',
    providers: [
      SessionPageStore,
    ],
    loadChildren: () => import('./sessions/routes').then(m => m.routes),
  },
  {
    path: 'quezaps',
    title: 'Quezaps',
    providers: [
      QuezapPageStore,
    ],
    loadChildren: () => import('./quezaps/routes').then(m => m.routes),
  },
  {
    path: 'themes',
    title: 'Thèmes',
    providers: [
      ThemePageStore,
    ],
    loadChildren: () => import('./themes/routes').then(m => m.routes),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
]
