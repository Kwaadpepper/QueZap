import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, map, of } from 'rxjs'

import { ValidationError } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { ServiceOutput } from '@quezap/core/types'

import { RegisterService } from './register'

@Injectable({ providedIn: 'root' })
export class RegisterHttpService implements RegisterService {
  private readonly http = inject(HttpClient)
  private readonly config = inject(Config)

  private get apiUrl(): string {
    return this.config.appConfig.value().apiUrl
  }

  register(email: string): ServiceOutput<void> {
    return this.http.post<void>(`${this.apiUrl}/users`, { name: email.split('@')[0], identifier: email, email }).pipe(
      map(() => ({ kind: 'success' as const, result: undefined })),
      catchError(error => of({
        kind: 'failure' as const,
        error: new ValidationError({ email: [error.message || 'Erreur lors de l\'enregistrement'] }, error.message),
      })),
    )
  }
}
