import { Routes } from '@angular/router'

import { routes as FeatureRoutes } from './features/routes'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home').then(m => m.Home),
  },
  {
    path: 'template',
    loadComponent: () => import('./template/template').then(m => m.Template),
  },
  ...FeatureRoutes,
]
