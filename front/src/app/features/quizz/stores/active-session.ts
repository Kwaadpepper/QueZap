import {
  computed, ErrorHandler, inject, Injector, isDevMode, runInInjectionContext,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import {
  patchState, signalState, SignalState, signalStore, withComputed, withHooks, withMethods, withState,
} from '@ngrx/signals'
import {
  catchError, concatMap, firstValueFrom,
  Observable, of, retry, switchMap, tap, throwError,
} from 'rxjs'

import { ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { ExpiredError } from '@quezap/core/errors/expired-error'
import { isFailure } from '@quezap/core/types'
import {
  MixedQuestion,
  Participant, QuestionWithAnswers, Session, SessionCode,
  sessionIsRunning, sessionMayStart,
} from '@quezap/domain/models'

import {
  NoMoreQuestions, SESSION_API_SERVICE,
  SESSION_OBSERVER_SERVICE, sessionEnded, sessionStarted,
} from '../services'

import { ActiveSessionPersistence } from './../services/active-session-persistence/active-session-persistence'

interface ActiveSessionState {
  _session: Session | undefined
  question: MixedQuestion & QuestionWithAnswers | NoMoreQuestions | undefined
  _nickname: SignalState<{
    value: string | undefined
    remembered: boolean
  }> | undefined
  _sessionIsLoaded: boolean
  _participants: Participant[]
}

const initialState: ActiveSessionState = {
  _session: undefined,
  question: undefined,
  _nickname: undefined,
  _sessionIsLoaded: false,
  _participants: [],
}

export const ActiveSessionStore = signalStore(
  withState(initialState),
  withComputed(store => ({
    session: computed(() => store._session?.()),
    nickname: computed(() => store._nickname?.()),
    restorationComplete: computed(() => store._sessionIsLoaded()),
    participants: computed(() => store._participants()),
    sessionIsRunning: computed(() => {
      const session = store._session?.()
      if (!session) {
        console.log('No active session found.')
      }
      return session ? sessionIsRunning(session) : false
    }),
  })),

  withMethods((
    store,
    sessionApi = inject(SESSION_API_SERVICE),
    activeSessionPersistence = inject(ActiveSessionPersistence),
  ) => ({

    joinSession: (code: SessionCode): Observable<void | NotFoundError | ExpiredError> => {
      return sessionApi.joinSessionWith(code).pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            patchState(store, initialState)
            patchState(store, { _sessionIsLoaded: true })

            const error = response.error
            if (error instanceof NotFoundError) {
              return of(error)
            }
            if (error instanceof ExpiredError) {
              return of(error)
            }

            return throwError(() => response.error)
          }

          const session = response.result

          if (sessionMayStart(session)) {
            console.debug('Session may start soon.')
          }

          if (sessionIsRunning(session)) {
            console.debug('Session is already running.')
          }

          patchState(store, { _session: session })
          activeSessionPersistence.patch({ code })
          return of(void 0)
        }),
        catchError((err) => {
          activeSessionPersistence.clear()
          patchState(store, initialState)
          patchState(store, { _sessionIsLoaded: true })

          return throwError(() => err)
        }),
      )
    },

    chooseNickname: (nickname: string, remember: boolean): Observable<void | ForbidenError | ValidationError> => {
      return sessionApi.chooseNickname(nickname).pipe(
        concatMap((response) => {
          if (isFailure(response)) {
            if (response.error instanceof ValidationError) {
              return of(response.error)
            }
            if (response.error instanceof ForbidenError) {
              return of(response.error)
            }

            activeSessionPersistence.persistNickname(remember ? nickname : undefined)

            patchState(store, { _nickname: undefined })

            return throwError(() => response.error)
          }

          activeSessionPersistence.persistNickname(remember ? nickname : undefined)

          patchState(store, {
            _nickname: signalState({
              value: nickname,
              remembered: remember,
            }),
          })

          return of(void 0)
        }),
        catchError((err) => {
          patchState(store, { _nickname: undefined })

          activeSessionPersistence.persistNickname()

          return throwError(() => err)
        }),
      )
    },
  })),

  withHooks({
    onInit(store,
      sessionObserver = inject(SESSION_OBSERVER_SERVICE),
      activeSessionPersistence = inject(ActiveSessionPersistence),
      injector = inject(Injector),
      errorHandler = inject(ErrorHandler),
    ) {
      const data = activeSessionPersistence.retrieve()
      if (data === null) {
        patchState(store, { _sessionIsLoaded: true })
        return
      }

      patchState(store, {
        ...initialState,
        _nickname: signalState({
          value: data.nickname?.trim(),
          remembered: data.nickname !== undefined && data.nickname.trim() !== '',
        }),
      })

      // * Run Listeners in injection context
      runInInjectionContext(injector, () => {
        console.log('Active session restored from persistence.')
        // * Listen to session status updates
        sessionObserver.participants().pipe(
          // Prevent memory leaks if the store is destroyed
          takeUntilDestroyed(),
          retry({ delay: 1000, count: Infinity }),
          tap((response) => {
            if (isDevMode()) {
              console.debug('Participants stream response:', JSON.stringify(response))
            }
            if (isFailure(response)) {
              const error = response.error
              console.error('Error in participants stream:', error)
              errorHandler.handleError(error)
              return of([])
            }

            const participants = response.result
            patchState(store, { _participants: participants })

            return response.result
          }),
          catchError((err) => {
            console.error('Error in participants stream:', err)
            errorHandler.handleError(err)
            return of([])
          }),
        ).subscribe()

        // * Listen to session status updates
        sessionObserver.sessionEvents().pipe(
          // Prevent memory leaks if the store is destroyed
          takeUntilDestroyed(),
          retry({ delay: 1000, count: Infinity }),
          concatMap((response) => {
            if (isDevMode()) {
              console.debug('Session events stream response:', JSON.stringify(response))
            }
            if (isFailure(response)) {
              const error = response.error
              console.error('Error listening to session status:', error)
              errorHandler.handleError(error)
              return of(void 0)
            }

            const session = store.session()
            const event = response.result

            if (!session) {
              return of(void 0)
            }

            if (sessionStarted(event)) {
              console.log('Session started event received.')
              patchState(store, {
                _session: {
                  ...session,
                  startedAt: event.session.startedAt,
                },
              })
            }

            if (sessionEnded(event)) {
              console.log('Session ended event received.')
              patchState(store, {
                _session: {
                  ...session,
                  endedAt: event.session.endedAt,
                },
              })
            }

            return of(void 0)
          }),
          catchError((err) => {
            console.error('Error listening to session status:', err)
            errorHandler.handleError(err)
            return of(void 0)
          }),
        ).subscribe()

        // * Listen to questions updates
        sessionObserver.questions().pipe(
          // Prevent memory leaks if the store is destroyed
          takeUntilDestroyed(),
          retry({ delay: 1000, count: Infinity }),
          switchMap((response) => {
            if (isDevMode()) {
              console.debug('Questions stream response:', JSON.stringify(response))
            }
            return isFailure(response)
              ? throwError(() => response.error)
              : of(response.result)
          }),
          tap((question) => {
            patchState(store, { question })
          }),
          catchError((err) => {
            console.error('Error listening to question status:', err)
            errorHandler.handleError(err)
            return of(void 0)
          }),
        ).subscribe()
      })

      firstValueFrom(
        store.joinSession(data.code).pipe(retry(1)),
      ).catch(() => {
        console.warn('Failed to restore active session from persistence, clearing persisted data.')
        activeSessionPersistence.clearKeepingNickname()
      }).finally(() => {
        patchState(store, { _sessionIsLoaded: true })
      })
    },
  }),
)
