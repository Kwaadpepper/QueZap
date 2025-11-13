import { ServiceError } from './service-error'

export class ForbidenError extends ServiceError {
  public static override readonly name: string = 'ForbidenError'
  public static override readonly code: number = 403

  public constructor(message = 'You do not have permission to access this resource.') {
    super(message)
  }
}
