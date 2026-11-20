import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, defer, from, map, of } from 'rxjs'

import { CreateUserRequest } from '@quezap/core/api-client'
import { create } from '@quezap/core/api-client/sdk.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'

import { RegisterService } from './register'

@Injectable({ providedIn: 'root' })
export class RegisterHttpService implements RegisterService {
  private readonly httpClient = inject(HttpClient, { optional: true })

  register(email: string): ServiceOutput<void> {
    const tempPassword = `Qz!9${globalThis.crypto?.randomUUID?.() ?? `${Date.now()}Aa1!`}`
    const payload: CreateUserRequest = {
      name: email.split('@')[0],
      identifier: email,
      email,
      password: tempPassword,
    }

    return defer(() =>
      from(
        create({
          body: payload,
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError(
              { email: ['Erreur lors de l\'enregistrement'] },
              'Erreur lors de l\'enregistrement',
            ),
          }
        }

        return { kind: 'success' as const, result: undefined }
      }),
      catchError((error: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            { email: [error instanceof Error ? error.message : 'Erreur lors de l\'enregistrement'] },
            error instanceof Error ? error.message : 'Erreur lors de l\'enregistrement',
          ),
        }),
      ),
    )
  }
}
