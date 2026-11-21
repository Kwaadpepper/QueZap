import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'

import { Observable, shareReplay, tap } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class SvgAssetLoader {
  private readonly httpClient = inject(HttpClient)
  private readonly cache = new Map<string, Observable<string>>()

  load(url: string): Observable<string> {
    const cached = this.cache.get(url)
    if (cached) {
      return cached
    }

    const request$ = this.httpClient.get(url, { responseType: 'text' }).pipe(
      tap({ error: () => this.cache.delete(url) }),
      shareReplay({ bufferSize: 1, refCount: false }),
    )
    this.cache.set(url, request$)
    return request$
  }
}
