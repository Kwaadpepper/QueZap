import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'licence',
    loadComponent: () => import('./licence/licence').then(m => m.Licence),
  },
  {
    path: 'about',
    loadComponent: () => import('./about/about').then(m => m.About),
  },
]
