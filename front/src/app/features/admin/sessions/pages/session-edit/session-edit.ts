import { Component, inject } from '@angular/core'
import { Router, ActivatedRoute } from '@angular/router'

import { Button } from 'primeng/button'

@Component({
  selector: 'quizz-session-edit',
  standalone: true,
  imports: [Button],
  template: `
    <div class="my-6 flex flex-col items-center justify-center gap-8">
      <h1 class="text-3xl font-bold">Gestion de la session</h1>
      <p class="text-xl">Une session ne peut pas être modifiée une fois créée.</p>
      <div class="flex gap-4">
        <p-button label="Retour aux sessions" severity="secondary" routerLink="/admin/sessions"></p-button>
        <p-button label="Projeter la session" severity="success" (onClick)="project()"></p-button>
      </div>
    </div>
  `,
})
export class SessionEdit {
  private router = inject(Router)
  private route = inject(ActivatedRoute)

  project() {
    const id = this.route.snapshot.paramMap.get('id')
    this.router.navigate(['/admin/sessions', id, 'present'])
  }
}
