import { Subject } from 'rxjs'
import * as zod from 'zod/v4'

import { zodToExternalValidationError } from '@quezap/core/tools/zod-to-external-validation-error'
import { ServiceOutput, Tried } from '@quezap/core/types'

import { AccountActivationService } from './activation-account'

const validationSchema = zod.jwt()

export class AccountActivationMockService implements AccountActivationService {
  private readonly MOCK_DELAY = () => Math.max(2000, Math.random() * 5000)

  activate(token: string): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    validationSchema
      .safeParseAsync(token)
      .then((result) => {
        setTimeout(() => {
          if (!result.success) {
            response.next(zodToExternalValidationError(result.error))
            response.complete()
            return
          }

          response.next({
            kind: 'success',
            result: undefined,
          })
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }
}
