import { Routes } from '@angular/router'

import { routes as AuthRoutes } from './features/auth/routes'
import { routes as FeatureRoutes } from './features/routes'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./home/home').then(m => m.Home),
  },
  {
    path: 'template',
    title: 'Deboggeur de template',
    loadComponent: () => import('./template/template').then(m => m.Template),
  },
  {
    path: 'auth',
    loadChildren: () => AuthRoutes,
  },
  ...FeatureRoutes,
]
