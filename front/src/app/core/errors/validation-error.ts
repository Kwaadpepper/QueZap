import { FieldTree, ValidationError as SignalValidationError } from '@angular/forms/signals'

import { Failure } from '../types'

import { ServiceError } from './service-error'

interface FormValidationError extends SignalValidationError {
  kind: 'external'
  message: string
}

export class ValidationError extends ServiceError implements Failure<ValidationError> {
  public static override readonly name: string = 'ValidationError'
  public static override readonly code: number = 422

  public override readonly error: ValidationError

  private readonly errors: Map<string, FormValidationError[]>

  public constructor(errors: Record<string, string[]>, message = 'The provided data is invalid.') {
    super(message)
    this.errors = new Map(
      Object.entries(errors)
        .map(([key, value]) => [key, value.map(this.errorMapper)]),
    )
    this.error = this
  }

  public getErrors(): Map<string, FormValidationError[]> {
    return new Map(
      [...this.errors.entries()]
        .map(([key, value]) => [key, value.map(this.copyError)]),
    )
  }

  public getErrorsForForm<T>(form: FieldTree<T>): SignalValidationError.WithOptionalField[] {
    const result: SignalValidationError.WithOptionalField[] = []

    for (const [field, errors] of this.errors.entries()) {
      const formField = form[field as keyof typeof form] ?? null
      for (const error of errors) {
        result.push({
          ...error,
          field: formField as FieldTree<unknown>,
        })
      }
    }

    return result
  }

  private errorMapper(message: string): FormValidationError {
    return {
      kind: 'external',
      message,
    }
  }

  private copyError(error: FormValidationError): FormValidationError {
    return { ...error }
  }
}
