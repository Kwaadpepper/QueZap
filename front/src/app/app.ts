import { Component, signal } from '@angular/core'
import { RouterOutlet } from '@angular/router'

interface City {
  name: string
  code: string
}

@Component({
  standalone: true,
  selector: 'quizz-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('quezap')
}
