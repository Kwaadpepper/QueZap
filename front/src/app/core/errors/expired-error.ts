import { Failure } from '../types'

import { ServiceError } from './service-error'

export class ExpiredError extends ServiceError implements Failure<ExpiredError> {
  public static override readonly name: string = 'ExpiredError'
  public static override readonly code: number = 410

  public override readonly error: ExpiredError

  public constructor(message = 'The resource you are trying to access has expired.') {
    super(message)
    this.error = this
  }
}
