import { bootstrapApplication } from '@angular/platform-browser'

import { App } from './app/app'
import { appConfig } from './app/app.config'

try {
  await bootstrapApplication(App, appConfig)
}
catch (err) {
  console.warn('--- Fatal error during application bootstrap ---')
  console.error(err)
}
