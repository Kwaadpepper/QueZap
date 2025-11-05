import { bootstrapApplication } from '@angular/platform-browser'

import { Environment } from '@quezap/core/services/config'

import { App } from './app/app'
import { appConfig } from './app/app.config'
import { environment } from './environments/environment'

try {
  await bootstrapApplication(App, appConfig)

  const title = document.querySelector('title')

  title!.text = environment.appName + (environment.env === Environment.PROD ? '' : ` [${environment.env}]`)
}
catch (err) {
  console.error(err)
}
