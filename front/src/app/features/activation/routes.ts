import { Routes } from '@angular/router'

export const routes: Routes = [
  {
    path: '',
    providers: [
    ],
    loadComponent: () => import('./account-activation/account-activation').then(m => m.AccountActivation),
  },
]
