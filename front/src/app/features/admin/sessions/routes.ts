import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages').then(m => m.SessionList),
  },
  {
    path: ':id/present',
    title: 'Projection de Session',
    loadComponent: () => import('./pages').then(m => m.SessionPresenter),
  },
]
