import { ZodError } from 'zod'

import { ExternalValidationError } from '../errors/external-validation-error'

export function zodToExternalValidationError(error: ZodError): ExternalValidationError {
  const validationErrors: Record<string, string[]> = {}

  for (const issue of error.issues) {
    const fieldPath = issue.path.join('.')
    if (!validationErrors[fieldPath]) {
      validationErrors[fieldPath] = []
    }
    validationErrors[fieldPath].push(issue.message)
  }

  return new ExternalValidationError(validationErrors)
}
