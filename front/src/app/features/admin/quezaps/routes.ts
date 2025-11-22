import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages').then(m => m.QuezapList),
  },
  {
    path: 'create',
    loadComponent: () => import('./pages').then(m => m.QuezapCreate),
  },
]
