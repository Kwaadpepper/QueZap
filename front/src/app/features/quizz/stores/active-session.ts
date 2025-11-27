import {
  computed, DestroyRef, effect, ErrorHandler, inject, Injector, isDevMode, runInInjectionContext,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

import {
  patchState, signalState, SignalState, signalStore, withComputed, withHooks, withMethods, withState,
} from '@ngrx/signals'
import {
  catchError, concatMap,
  Observable, of,
  retry,
  throwError,
} from 'rxjs'

import { ForbidenError, NotFoundError, ValidationError } from '@quezap/core/errors'
import { ExpiredError } from '@quezap/core/errors/expired-error'
import { isFailure } from '@quezap/core/types'
import {
  MixedQuestion,
  Participant, QuestionWithAnswers, Session, SessionCode,
  sessionHasStarted, sessionMayStart,
} from '@quezap/domain/models'

import {
  NoMoreQuestions, SESSION_API_SERVICE,
  SESSION_OBSERVER_SERVICE, sessionEnded, sessionStarted,
  sessionWaitingStart,
  WaitingQuestion,
} from '../services'

import { ActiveSessionPersistence } from './../services/active-session-persistence/active-session-persistence'

interface ActiveSessionState {
  _session: Session | undefined
  question: WaitingQuestion | MixedQuestion & QuestionWithAnswers | NoMoreQuestions | undefined
  _nickname: SignalState<{
    value: string | undefined
    remembered: boolean
  }> | undefined
  _sessionIsLoaded: boolean
  _sessionIsRunning: boolean
  _participants: Participant[]
}

const initialState: ActiveSessionState = {
  _session: undefined,
  question: undefined,
  _nickname: undefined,
  _sessionIsLoaded: false,
  _sessionIsRunning: false,
  _participants: [],
}

export const ActiveSessionStore = signalStore(
  withState(initialState),
  withComputed(store => ({
    session: computed(() => store._session?.()),
    nickname: computed(() => store._nickname?.()),
    restorationComplete: computed(() => store._sessionIsLoaded()),
    participants: computed(() => store._participants()),
    sessionHasStarted: computed(() => store._sessionIsRunning()),
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

          if (sessionHasStarted(session)) {
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
      destroyRef = inject(DestroyRef),
      injector = inject(Injector),
      errorHandler = inject(ErrorHandler),
    ) {
      // * Run Listeners in injection context
      runInInjectionContext(injector, () => {
        if (isDevMode()) {
          console.debug('Running listeners')
        }

        // * React to participants updates
        effect(() => {
          const participants = sessionObserver.participants()
          console.debug('Updating participants list from session observer', participants)
          patchState(store, { _participants: participants })
        })

        // * React to question updates
        effect(() => {
          const question = sessionObserver.question()
          console.debug('Updating current question from session observer', question)
          patchState(store, { question })
        })

        // * Listen to session status updates
        sessionObserver.sessionEvents().pipe(
          // Prevent memory leaks if the store is destroyed
          takeUntilDestroyed(destroyRef),
          retry({ count: Infinity, delay: 1000 }),
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

            if (sessionWaitingStart(event)) {
              patchState(store, {
                _session: { ...session },
                _sessionIsRunning: false,
              })
            }

            if (sessionStarted(event)) {
              patchState(store, {
                _session: {
                  ...session,
                  startedAt: event.session.startedAt,
                },
                _sessionIsRunning: true,
              })
            }

            if (sessionEnded(event)) {
              patchState(store, {
                _session: {
                  ...session,
                  endedAt: event.session.endedAt,
                },
                _sessionIsRunning: false,
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
      })

      const data = activeSessionPersistence.retrieve()
      if (data === null) {
        patchState(store, { _sessionIsLoaded: true })
        return
      }

      if (isDevMode()) {
        console.debug('Restoring active session from persistence:', data)
      }

      patchState(store, {
        ...initialState,
        _nickname: signalState({
          value: data.nickname?.trim(),
          remembered: data.nickname !== undefined && data.nickname.trim() !== '',
        }),
      })

      patchState(store, { _sessionIsLoaded: true })
    },
  }),
)
