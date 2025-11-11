import { Observable, Subject } from 'rxjs'

import { zod, zodToExternalValidationError } from '@quezap/core/tools'

import { AccountActivationService } from './activation-account'

const validationSchema = zod.jwt()

export class AccountActivationMockService implements AccountActivationService {
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)

  activate(token: string): Observable<void> {
    const response = new Subject<void>()

    validationSchema
      .safeParseAsync(token)
      .then((result) => {
        setTimeout(() => {
          if (!result.success) {
            response.error(
              zodToExternalValidationError(result.error),
            )
            return
          }

          response.next()
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }
}
