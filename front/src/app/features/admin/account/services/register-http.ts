import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { CreateUserRequest } from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'

import { RegisterService } from './register'

@Injectable({ providedIn: 'root' })
export class RegisterHttpService implements RegisterService {
  register(email: string): ServiceOutput<void> {
    const payload: CreateUserRequest = {
      name: email.split('@')[0],
      identifier: email,
      email,
    }

    return from(
      client.post<void>({
        url: '/apiv1/users',
        body: payload,
      }),
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
