import { ValidationError as SignalValidationError } from '@angular/forms/signals'

interface ValidationError extends SignalValidationError {
  kind: 'external'
  message: string
}

export class ExternalValidationError extends Error {
  private readonly errors: Map<string, ValidationError[]>

  public constructor(errors: Record<string, string[]>) {
    super('Validation error')
    this.errors = new Map(
      Object.entries(errors)
        .map(([key, value]) => [key, value.map(this.errorMapper)]),
    )
  }

  public getErrors(): Map<string, ValidationError[]> {
    return new Map(
      [...this.errors.entries()]
        .map(([key, value]) => [key, value.map(this.copyError)]),
    )
  }

  private errorMapper(message: string): ValidationError {
    return {
      kind: 'external',
      message,
    }
  }

  private copyError(error: ValidationError): ValidationError {
    return { ...error }
  }
}
