import { Failure } from '../types'

export class ServiceError extends Error implements Failure<ServiceError> {
  public static override readonly name: string = 'ServiceError'
  public static readonly code: number = 0
  public readonly kind = 'failure'
  public readonly error: ServiceError

  constructor(message: string) {
    super(message)
    this.error = this
  }
}
