import { Injectable } from '@angular/core'

import { catchError, from, map, of } from 'rxjs'

import { ActivateRequest, MessageResponseDto } from '@quezap/core/api-client'
import { client } from '@quezap/core/api-client/client.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'

import { AccountActivationService } from './activation-account'

@Injectable({ providedIn: 'root' })
export class AccountActivationHttpService implements AccountActivationService {
  activate(token: string): ServiceOutput<void> {
    const payload: ActivateRequest = { token }

    return from(
      client.post<MessageResponseDto>({
        url: '/apiv1/auth/activate',
        body: payload,
      }),
    ).pipe(
      map(({ error }) => {
        if (error) {
          return {
            kind: 'failure' as const,
            error: new ValidationError({}, 'Erreur lors de l\'activation'),
          }
        }

        return {
          kind: 'success' as const,
          result: undefined,
        }
      }),
      catchError((err: unknown) =>
        of({
          kind: 'failure' as const,
          error: new ValidationError(
            {},
            err instanceof Error ? err.message : 'Erreur lors de l\'activation',
          ),
        }),
      ),
    )
  }
}
