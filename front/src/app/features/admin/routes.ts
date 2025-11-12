import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'dashboard',
    title: 'Tableau de bord',
    loadChildren: () => import('./dashboard/routes').then(m => m.routes),
  },
  {
    path: 'themes',
    title: 'Thèmes',
    loadChildren: () => import('./themes/routes').then(m => m.routes),
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
]
