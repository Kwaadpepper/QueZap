import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: 'licence',
    title: 'Licence',
    loadComponent: () => import('./licence/licence').then(m => m.Licence),
  },
  {
    path: 'about',
    title: 'À propos',
    loadComponent: () => import('./about/about').then(m => m.About),
  },
]
