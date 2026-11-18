import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { catchError, defer, from, map, of } from 'rxjs'

import { ActivateRequest } from '@quezap/core/api-client'
import { activate } from '@quezap/core/api-client/sdk.gen'
import { ValidationError } from '@quezap/core/errors'
import { ServiceOutput } from '@quezap/core/types'

import { AccountActivationService } from './activation-account'

@Injectable({ providedIn: 'root' })
export class AccountActivationHttpService implements AccountActivationService {
  private readonly httpClient = inject(HttpClient, { optional: true })

  activate(token: string): ServiceOutput<void> {
    const payload: ActivateRequest = { token }

    return defer(() =>
      from(
        activate({
          body: payload,
          ...(this.httpClient ? { httpClient: this.httpClient } : {}),
        }),
      ),
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
