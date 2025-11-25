import { of } from 'rxjs'
import * as zod from 'zod/v4'
import fr from 'zod/v4/locales/fr.js'

export function initZod() {
  return of(zod.config(fr()))
}
