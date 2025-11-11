import { FieldTree, ValidationError as SignalValidationError, ValidationErrorWithOptionalField } from '@angular/forms/signals'

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

  public getErrorsForForm<T>(form: FieldTree<T>): ValidationErrorWithOptionalField[] {
    const result: ValidationErrorWithOptionalField[] = []

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
