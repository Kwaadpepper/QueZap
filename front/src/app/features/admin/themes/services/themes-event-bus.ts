import { Injectable } from '@angular/core'

import { Subject } from 'rxjs'

import type { ThemeId } from '@quezap/domain/models'

@Injectable({ providedIn: 'root' })
export class ThemesEventsBus {
  readonly themeChanged = new Subject<ThemeId>()

  dispatchChanged(theme: ThemeId): void {
    this.themeChanged.next(theme)
  }
}
