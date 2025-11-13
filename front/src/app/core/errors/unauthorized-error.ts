import { Failure } from '../types'

import { ServiceError } from './service-error'

export class UnauthorizedError extends ServiceError implements Failure<UnauthorizedError> {
  public static override readonly name: string = 'UnauthorizedError'
  public static override readonly code: number = 401

  public override readonly error: UnauthorizedError

  public constructor(message = 'Unauthorized access to this resource.') {
    super(message)
    this.error = this
  }
}
