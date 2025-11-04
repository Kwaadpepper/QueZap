import { JsonPipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';

type City = {
    name: string;
    code: string;
}

@Component({
  standalone: true,
  selector: 'app-root',
  imports: [ButtonModule, FormsModule, MultiSelectModule, JsonPipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('quezap');

    cities: City[] = [
    {name: 'New York', code: 'NY'},
    {name: 'Rome', code: 'RM'},
    {name: 'London', code: 'LDN'},
    {name: 'Istanbul', code: 'IST'},
    {name: 'Paris', code: 'PRS'}
  ]

  selectedCities: City[] = [];

  doSomething() {
    console.log('Button clicked!');
  }
}
