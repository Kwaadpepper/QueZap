import { ZodError } from 'zod/v4'

import { ValidationError } from '../errors/validation-error'

export function zodToExternalValidationError(error: ZodError): ValidationError {
  const validationErrors: Record<string, string[]> = {}

  for (const issue of error.issues) {
    const fieldPath = issue.path.join('.')
    if (!validationErrors[fieldPath]) {
      validationErrors[fieldPath] = []
    }
    validationErrors[fieldPath].push(issue.message)
  }

  return new ValidationError(validationErrors)
}
