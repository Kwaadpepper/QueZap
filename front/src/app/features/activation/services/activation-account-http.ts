import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { NotFoundError, ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceOutput } from '@quezap/core/types'

import { AccountActivationService } from './activation-account'

@Injectable({ providedIn: 'root' })
export class AccountActivationHttpService implements AccountActivationService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  activate(token: string): ServiceOutput<void> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/auth/activate`, { token }).pipe(
      map(() => ({
        kind: 'success' as const,
        result: undefined,
      })),
      catchError((err: HttpErrorResponse) => of({
        kind: 'failure' as const,
        error: err.status === 404
          ? new NotFoundError(err.error?.message || 'Jeton d\'activation introuvable ou invalide')
          : new ValidationError({}, err.error?.message || 'Erreur lors de l\'activation'),
      })),
    )
  }
}
