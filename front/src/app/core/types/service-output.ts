import { Observable } from 'rxjs'

import { ServiceError } from '../errors'

export type Tried<Result, E extends ServiceError = ServiceError> = Success<Result> | Failure<E>
interface Success<R> { kind: 'success', result: R }
export interface Failure<E> { kind: 'failure', error: E }

export type ServiceOutput<T, E extends ServiceError = ServiceError> = Observable<Tried<T, E | ServiceError>>

export function isSuccess<T, E extends ServiceError>(tried: Tried<T, E>): tried is Success<T> {
  return tried.kind === 'success'
}

export function isFailure<T, E extends ServiceError>(tried: Tried<T, E>): tried is Failure<E> {
  return tried.kind === 'failure'
}
