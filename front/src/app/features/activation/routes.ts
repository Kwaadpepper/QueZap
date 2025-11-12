import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./account-activation/account-activation').then(m => m.AccountActivation),
  },
]
