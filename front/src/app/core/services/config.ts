import { Injectable } from '@angular/core'

import z from 'zod'

import { environment } from '../../../environments/environment'

export enum Environment {
  DEV = 'dev',
  PROD = 'prod',
}

const configSchema = z.object({
  env: z.enum(Environment),
  apiUrl: z.url(),
})

type AppConfig = z.infer<typeof configSchema>

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private readonly appConfig!: AppConfig

  constructor() {
    this.appConfig = configSchema.parse(environment)
  }

  getConfig(): AppConfig {
    return this.appConfig
  }
}
