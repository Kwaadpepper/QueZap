import { Component, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'

import { Button } from 'primeng/button'

@Component({
  selector: 'quizz-session-edit',
  standalone: true,
  imports: [Button],
  template: `
    <div class="my-6 flex flex-col items-center justify-center gap-8">
      <h1 class="text-3xl font-bold">Gestion de la session</h1>
      <p class="text-xl text-surface-600 dark:text-surface-300">
        Une session ne peut pas être modifiée une fois créée.
      </p>
      <div class="flex gap-4">
        <p-button label="Retour aux sessions" severity="secondary" (onClick)="back()"></p-button>
        <p-button label="Projeter la session" severity="success" (onClick)="project()"></p-button>
      </div>
    </div>
  `,
})
export class SessionEdit {
  private readonly router = inject(Router)
  private readonly route = inject(ActivatedRoute)

  back(): void {
    this.router.navigate(['/admin/sessions'])
  }

  project(): void {
    const id = this.route.snapshot.paramMap.get('id')
    if (id) {
      const url = this.router.serializeUrl(this.router.createUrlTree(['/projector', id]))
      window.open(url, '_blank')
    }
  }
}
