import '@angular/compiler'
import 'zone.js'
import 'zone.js/testing'
import { provideHttpClient } from '@angular/common/http'
import { ErrorHandler } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'

import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone'
import { provideMarkdown } from 'ngx-markdown'
import { ConfirmationService, MessageService } from 'primeng/api'

import { createErrorNotifier, ERROR_NOTIFIER, GlobalErrorHandler } from '@quezap/core/errors'
import { Config } from '@quezap/core/services/config/config'
import { LayoutSettings } from '@quezap/core/services/layout/layout-settings'
import { ACCOUNT_ACTIVATION_SERVICE, AccountActivationMockService } from '@quezap/features/activation/services'
import { REGISTER_SERVICE, RegisterMockService } from '@quezap/features/admin/account/services'
import { QUEZAP_SERVICE, QuezapMockService } from '@quezap/features/admin/quezaps/services'
import { QuezapEditorStore, QuezapPageStore } from '@quezap/features/admin/quezaps/stores'
import { SESSION_SERVICE, SessionMockService } from '@quezap/features/admin/sessions/services'
import { SessionPageStore } from '@quezap/features/admin/sessions/stores'
import { THEME_SERVICE, ThemeMockService } from '@quezap/features/admin/themes/services'
import { ThemePageStore } from '@quezap/features/admin/themes/stores'
import { AUTHENTICATION_SERVICE, AuthenticationMockService } from '@quezap/features/auth/services'
import {
  ActiveSessionPersistence, SESSION_API_SERVICE, SESSION_OBSERVER_SERVICE,
  SessionApiMockService, SessionMocks, SessionObserverMockService,
} from '@quezap/features/quizz/services'
import { ActiveSessionStore, TimerStore } from '@quezap/features/quizz/stores'

setupZoneTestEnv()

jest.setTimeout(15000)

global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Object.defineProperty(HTMLElement.prototype, 'inert', {
  get() {
    return this.hasAttribute('inert')
  },
  set(val: boolean) {
    if (val) {
      this.setAttribute('inert', '')
    } else {
      this.removeAttribute('inert')
    }
  },
  configurable: true,
})

beforeEach(() => {
  TestBed.configureTestingModule({
    providers: [
      MessageService,
      ConfirmationService,
      Config,
      LayoutSettings,
      ActiveSessionStore,
      ActiveSessionPersistence,
      TimerStore,
      SessionMocks,
      SessionPageStore,
      QuezapPageStore,
      QuezapEditorStore,
      ThemePageStore,
      { provide: ERROR_NOTIFIER, useFactory: createErrorNotifier, deps: [MessageService] },
      { provide: ErrorHandler, useClass: GlobalErrorHandler },
      { provide: ACCOUNT_ACTIVATION_SERVICE, useClass: AccountActivationMockService },
      { provide: AUTHENTICATION_SERVICE, useClass: AuthenticationMockService },
      { provide: REGISTER_SERVICE, useClass: RegisterMockService },
      { provide: SESSION_API_SERVICE, useClass: SessionApiMockService },
      { provide: SESSION_OBSERVER_SERVICE, useClass: SessionObserverMockService },
      { provide: SESSION_SERVICE, useClass: SessionMockService },
      { provide: QUEZAP_SERVICE, useClass: QuezapMockService },
      { provide: THEME_SERVICE, useClass: ThemeMockService },
      provideMarkdown(),
      provideHttpClient(),
      provideRouter([]),
    ],
  })
})
