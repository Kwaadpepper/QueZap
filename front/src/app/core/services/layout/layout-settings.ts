import { Injectable, signal } from '@angular/core'

@Injectable({
  providedIn: 'root',
})
export class LayoutSettings {
  public readonly inContainer = signal(true)
  public readonly asWebsite = signal(true)
}
