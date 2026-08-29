import { provideHttpClient, withInterceptors } from '@angular/common/http'
import {
  ApplicationConfig, enableProdMode, ErrorHandler, isDevMode, provideAppInitializer,
  provideBrowserGlobalErrorListeners, provideZonelessChangeDetection,
} from '@angular/core'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter, TitleStrategy, withComponentInputBinding } from '@angular/router'
import { provideServiceWorker } from '@angular/service-worker'

import { MessageService } from 'primeng/api'
import { providePrimeNG } from 'primeng/config'

import { environment } from '@quezap/env/environment'
import Quezap from '@quezap/themes/Quezap'

import { AppInitializer } from './app.initializer'
import { routes } from './app.routes'
import { createErrorNotifier, ERROR_NOTIFIER, GlobalErrorHandler } from './core/errors'
import { loadingProgressionInterceptor } from './core/interceptors/loading-progression/loading-progression-interceptor'
import { refreshSessionInterceptor } from './core/interceptors/refresh-session/refresh-session-interceptor'
import { Config } from './core/services/config/config'
import { DynamicTitleStrategy } from './core/strategies/dynamic-title'
import {
  ACCOUNT_ACTIVATION_SERVICE,
  AccountActivationHttpService,
  AccountActivationMockService,
} from './features/activation/services'
import { REGISTER_SERVICE, RegisterHttpService, RegisterMockService } from './features/admin/account/services'
import { QUEZAP_SERVICE, QuezapHttpService, QuezapMockService } from './features/admin/quezaps/services'
import { SESSION_SERVICE, SessionHttpService, SessionMockService } from './features/admin/sessions/services'
import { THEME_SERVICE, ThemeHttpService, ThemeMockService } from './features/admin/themes/services'
import { AUTHENTICATION_SERVICE, AuthenticationHttpService, AuthenticationMockService } from './features/auth/services'
import {
  SESSION_API_SERVICE, SESSION_OBSERVER_SERVICE,
  SessionApiHttpService, SessionApiMockService,
  SessionObserverMockService, SessionObserverSseService,
} from './features/quizz/services'

if (environment.env === 'prod') {
  enableProdMode()
}

const appProviders: ApplicationConfig['providers'] = [
  MessageService,
  { provide: Config, useClass: Config },
  { provide: ERROR_NOTIFIER, useFactory: createErrorNotifier, deps: [MessageService] },
  { provide: ErrorHandler, useClass: GlobalErrorHandler },
  {
    provide: AUTHENTICATION_SERVICE,
    useClass: environment.useMockData ? AuthenticationMockService : AuthenticationHttpService,
  },
  {
    provide: ACCOUNT_ACTIVATION_SERVICE,
    useClass: environment.useMockData ? AccountActivationMockService : AccountActivationHttpService,
  },
  {
    provide: REGISTER_SERVICE,
    useClass: environment.useMockData ? RegisterMockService : RegisterHttpService,
  },
  {
    provide: THEME_SERVICE,
    useClass: environment.useMockData ? ThemeMockService : ThemeHttpService,
  },
  {
    provide: QUEZAP_SERVICE,
    useClass: environment.useMockData ? QuezapMockService : QuezapHttpService,
  },
  {
    provide: SESSION_SERVICE,
    useClass: environment.useMockData ? SessionMockService : SessionHttpService,
  },
  {
    provide: SESSION_API_SERVICE,
    useClass: environment.useMockData ? SessionApiMockService : SessionApiHttpService,
  },
  {
    provide: SESSION_OBSERVER_SERVICE,
    useClass: environment.useMockData ? SessionObserverMockService : SessionObserverSseService,
  },
  { provide: TitleStrategy, useClass: DynamicTitleStrategy },
]

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([loadingProgressionInterceptor, refreshSessionInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Quezap,
        options: {
          cssLayer: {
            name: 'primeng',
            order: 'theme, base, primeng',
          },
        },
      },
    }),
    provideAppInitializer(AppInitializer),
    provideServiceWorker('ngsw-worker.js', {
      enabled: environment.env === 'prod' || !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    ...appProviders,
  ],
}
