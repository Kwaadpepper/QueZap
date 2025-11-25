import { Subject } from 'rxjs'

import { zod } from '@quezap/core/tools/zod'
import { zodToExternalValidationError } from '@quezap/core/tools/zod-to-external-validation-error'
import { ServiceOutput, Tried } from '@quezap/core/types'

import { RegisterService } from './register'

const validationSchema = zod.object({ email: zod.email().check(zod.regex(/@quezap\.com$/)) })

export class RegisterMockService implements RegisterService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)

  register(email: string): ServiceOutput<void> {
    const response = new Subject<Tried<void>>()

    validationSchema
      .safeParseAsync({ email })
      .then((result) => {
        if (!result.success) {
          response.next(zodToExternalValidationError(result.error))
          response.complete()
          return
        }

        setTimeout(() => {
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
