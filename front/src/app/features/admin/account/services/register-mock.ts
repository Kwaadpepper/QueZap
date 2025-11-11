import { Observable, Subject } from 'rxjs'

import { zod } from '@quezap/core/tools'
import { zodToExternalValidationError } from '@quezap/core/tools/zod-to-external-validation-error'

import { RegisterService } from './register'

const validationSchema = zod.object({
  email: zod.email().check(zod.regex(/@quezap\.com$/)),
  username: zod.string().min(3).max(20),
})

export class RegisterMockService implements RegisterService {
  private readonly MOCK_DELAY = () => Math.max(100, Math.random() * 3000)

  register(email: string, username: string): Observable<void> {
    const response = new Subject<void>()

    validationSchema
      .safeParseAsync({ email, username })
      .then((result) => {
        if (!result.success) {
          response.error(
            zodToExternalValidationError(result.error),
          )
          return
        }

        setTimeout(() => {
          response.next()
          response.complete()
        }, this.MOCK_DELAY())
      })

    return response
  }
}
