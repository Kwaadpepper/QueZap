import { Failure } from '../types'

import { ServiceError } from './service-error'

export class ForbidenError extends ServiceError implements Failure<ForbidenError> {
  public static override readonly name: string = 'ForbidenError'
  public static override readonly code: number = 403

  public override readonly error: ForbidenError

  public constructor(message = 'You do not have permission to access this resource.') {
    super(message)
    this.error = this
  }
}
