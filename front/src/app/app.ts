import { JsonPipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { FieldsetModule } from 'primeng/fieldset';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MultiSelectModule } from 'primeng/multiselect';
import { PanelModule } from 'primeng/panel';
import { ProgressBarModule } from 'primeng/progressbar';
import { RadioButtonModule } from 'primeng/radiobutton';
import { RatingModule } from 'primeng/rating';
import { SelectModule } from 'primeng/select';
import { SliderModule } from 'primeng/slider';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

type City = {
  name: string;
  code: string;
}

type Plant = {
  name: string;
  family: string;
  difficulty: string;
}

@Component({
  selector: 'app-root',
  imports: [
    AccordionModule,
    AutoCompleteModule,
    BadgeModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    DatePickerModule,
    DialogModule,
    DividerModule,
    FieldsetModule,
    FormsModule,
    InputNumberModule,
    InputTextModule,
    JsonPipe,
    MessageModule,
    MultiSelectModule,
    PanelModule,
    ProgressBarModule,
    RadioButtonModule,
    RatingModule,
    SelectModule,
    SliderModule,
    TabsModule,
    TagModule,
    ToastModule,
    ToggleSwitchModule,
  ],
  providers: [MessageService],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('quezap');

  cities: City[] = [
    { name: 'New York', code: 'NY' },
    { name: 'Rome', code: 'RM' },
    { name: 'London', code: 'LDN' },
    { name: 'Istanbul', code: 'IST' },
    { name: 'Paris', code: 'PRS' }
  ];

  plants: Plant[] = [
    { name: 'Rose', family: 'Rosaceae', difficulty: 'Facile' },
    { name: 'Orchidée', family: 'Orchidaceae', difficulty: 'Difficile' },
    { name: 'Cactus', family: 'Cactaceae', difficulty: 'Facile' },
    { name: 'Fougère', family: 'Polypodiaceae', difficulty: 'Moyen' },
    { name: 'Bambou', family: 'Poaceae', difficulty: 'Moyen' }
  ];

  selectedCities: City[] = [];
  filteredPlants: Plant[] = [];
  checked = false;
  selectedDifficulty = 'Facile';
  switchValue = false;
  rating = 3;
  sliderValue = 50;
  numberValue = 42;
  textareaValue = '';
  visible = false;

  constructor(private readonly messageService: MessageService) {}

  doSomething() {
    console.log('Button clicked!');
  }

  showToast(severity: 'success' | 'info' | 'warn' | 'error') {
    const messages = {
      success: { summary: 'Succès', detail: 'Opération réussie !' },
      info: { summary: 'Information', detail: 'Voici une information importante.' },
      warn: { summary: 'Attention', detail: 'Soyez vigilant avec cette action.' },
      error: { summary: 'Erreur', detail: 'Une erreur est survenue.' }
    };

    this.messageService.add({
      severity,
      ...messages[severity],
      life: 3000
    });
  }

  showDialog() {
    this.visible = true;
  }

  searchPlants(event: { query: string }) {
    this.filteredPlants = this.plants.filter(plant =>
      plant.name.toLowerCase().includes(event.query.toLowerCase())
    );
  }
}
