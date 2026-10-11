import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages').then(m => m.SessionList),
  },
  {
    path: 'create',
    title: 'Création de Session',
    loadComponent: () => import('./pages').then(m => m.SessionCreate),
  },
  {
    path: ':id/edit',
    title: 'Édition de Session',
    loadComponent: () => import('./pages').then(m => m.SessionEdit),
  },
  {
    path: ':id/present',
    title: 'Projection de Session',
    loadComponent: () => import('./pages').then(m => m.SessionPresenter),
  },
  {
    path: ':id/report',
    title: 'Rapport de Session',
    loadComponent: () => import('./pages').then(m => m.SessionReport),
  },
]
