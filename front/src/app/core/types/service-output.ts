import { Signal } from '@angular/core'

import { Observable } from 'rxjs'

import { ServiceError } from '../errors'

export type Tried<Result, E extends ServiceError = ServiceError> = Success<Result> | Failure<E>
interface Success<R> { readonly kind: 'success', readonly result: R }
export interface Failure<E> { readonly kind: 'failure', readonly error: E }
export type ServiceOutput<T, E extends ServiceError = ServiceError> = Observable<Tried<T, E | ServiceError>>

/** A service that does not complete */
export type ServiceObservable<T> = Observable<Tried<T, ServiceError>>
export type ServiceState<T> = Signal<T>

export function isSuccess<T, E extends ServiceError>(tried: Tried<T, E>): tried is Success<T> {
  return tried.kind === 'success'
}

export function isFailure<T, E extends ServiceError>(tried: Tried<T, E>): tried is Failure<E> {
  return tried.kind === 'failure'
}
