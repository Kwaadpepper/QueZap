import { ValidationError } from '@angular/forms/signals'

import * as zod from 'zod/v4'

export function superRefineExternalError<T>(externalErrors: () => ValidationError[] | undefined) {
  return (val: T, ctx: zod.core.$RefinementCtx<T>) => {
    for (const error of externalErrors() ?? []) {
      if (error.message !== undefined) {
        ctx.addIssue({
          code: 'custom',
          message: error.message,
        })
      }
    }
  }
}
