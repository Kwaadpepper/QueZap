import { HttpErrorResponse } from '@angular/common/http'

export class HandledFrontError extends Error {
  public static override readonly name: string = 'AsyncFrontError'

  constructor(message = 'An asynchronous front-end error occurred.', cause: Error | undefined = undefined) {
    super(message, { cause })
  }

  public static from(unknown: unknown): HandledFrontError {
    if (unknown instanceof HandledFrontError) {
      return unknown
    }

    if (unknown instanceof HttpErrorResponse) {
      return new HandledFrontError(unknown.message, unknown)
    }

    if (unknown instanceof Error) {
      return new HandledFrontError(unknown.message, unknown)
    }

    console.error(unknown)
    throw new Error('Cannot create HandledFrontError from non-error value.')
  }
}
