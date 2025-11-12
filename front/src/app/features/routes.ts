import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'admin',
    title: 'Administration',
    loadChildren: () => import('./admin/routes').then(m => m.routes),
  },
  {
    path: 'auth',
    title: 'Authentification',
    loadChildren: () => import('./auth/routes').then(m => m.routes),
  },
  {
    path: 'activation',
    title: 'Activation du compte',
    loadChildren: () => import('./activation/routes').then(m => m.routes),
  },
]
