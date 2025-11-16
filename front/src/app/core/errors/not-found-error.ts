import { Failure } from '../types'

import { ServiceError } from './service-error'

export class NotFoundError extends ServiceError implements Failure<NotFoundError> {
  public static override readonly name: string = 'NotFoundError'
  public static override readonly code: number = 404

  public override readonly error: NotFoundError

  public constructor(message = 'The requested resource was not found.') {
    super(message)
    this.error = this
  }
}
