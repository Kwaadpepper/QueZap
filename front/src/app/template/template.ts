import { JsonPipe } from '@angular/common'
import { Component, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'

import { AccordionModule } from 'primeng/accordion'
import { MessageService } from 'primeng/api'
import { AutoCompleteModule } from 'primeng/autocomplete'
import { BadgeModule } from 'primeng/badge'
import { ButtonModule } from 'primeng/button'
import { CardModule } from 'primeng/card'
import { CheckboxModule } from 'primeng/checkbox'
import { DatePickerModule } from 'primeng/datepicker'
import { DialogModule } from 'primeng/dialog'
import { DividerModule } from 'primeng/divider'
import { FieldsetModule } from 'primeng/fieldset'
import { ImageModule } from 'primeng/image'
import { InputNumberModule } from 'primeng/inputnumber'
import { InputTextModule } from 'primeng/inputtext'
import { MessageModule } from 'primeng/message'
import { MultiSelectModule } from 'primeng/multiselect'
import { PanelModule } from 'primeng/panel'
import { ProgressBarModule } from 'primeng/progressbar'
import { RadioButtonModule } from 'primeng/radiobutton'
import { RatingModule } from 'primeng/rating'
import { SelectModule } from 'primeng/select'
import { SliderModule } from 'primeng/slider'
import { TabsModule } from 'primeng/tabs'
import { TagModule } from 'primeng/tag'
import { ToastModule } from 'primeng/toast'
import { ToggleSwitchModule } from 'primeng/toggleswitch'

interface City {
  name: string
  code: string
}

interface Plant {
  name: string
  family: string
  difficulty: string
}

@Component({
  selector: 'quizz-template',
  imports: [
    RouterModule,
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
    ImageModule,
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
  templateUrl: './template.html',
})
export class Template {
  protected readonly title = signal('quezap')

  protected readonly colorShades: number[] = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950]

  protected readonly fullColorNames: string[] = [
    'primary', // Couleur principale (selon le thème PrimeNG)
    'surface', // Couleur de surface (selon le thème PrimeNG)
    'red', 'orange', 'amber', 'yellow', 'lime', 'green', 'emerald', 'teal', 'cyan',
    'sky', 'blue', 'indigo', 'violet', 'purple', 'fuchsia', 'pink', 'rose',
    'slate', 'gray', 'zinc', 'neutral', 'stone',
  ]

  protected readonly cities: City[] = [
    { name: 'New York', code: 'NY' },
    { name: 'Rome', code: 'RM' },
    { name: 'London', code: 'LDN' },
    { name: 'Istanbul', code: 'IST' },
    { name: 'Paris', code: 'PRS' },
  ]

  protected readonly plants: Plant[] = [
    { name: 'Rose', family: 'Rosaceae', difficulty: 'Facile' },
    { name: 'Orchidée', family: 'Orchidaceae', difficulty: 'Difficile' },
    { name: 'Cactus', family: 'Cactaceae', difficulty: 'Facile' },
    { name: 'Fougère', family: 'Polypodiaceae', difficulty: 'Moyen' },
    { name: 'Bambou', family: 'Poaceae', difficulty: 'Moyen' },
  ]

  protected readonly selectedCities = signal<City[]>([])
  protected readonly filteredPlants = signal<Plant[]>([])
  protected readonly checked = false
  protected readonly selectedDifficulty = 'Facile'
  protected readonly switchValue = false
  protected readonly rating = 3
  protected readonly sliderValue = 50
  protected readonly numberValue = 42
  protected readonly textareaValue = ''
  protected readonly visible = signal(false)

  private readonly messageService = inject(MessageService)

  doSomething() {
    console.log('Button clicked!')
  }

  showToast(severity: 'success' | 'info' | 'warn' | 'error') {
    const messages = {
      success: { summary: 'Succès', detail: 'Opération réussie !' },
      info: { summary: 'Information', detail: 'Voici une information importante.' },
      warn: { summary: 'Attention', detail: 'Soyez vigilant avec cette action.' },
      error: { summary: 'Erreur', detail: 'Une erreur est survenue.' },
    }

    this.messageService.add({
      severity,
      ...messages[severity],
      life: 3000,
    })
  }

  showDialog() {
    this.visible.set(true)
  }

  searchPlants(event: { query: string }) {
    this.filteredPlants.set(
      this.plants.filter(plant =>
        plant.name.toLowerCase().includes(event.query.toLowerCase())
        || plant.family.toLowerCase().includes(event.query.toLowerCase()),
      ),
    )
  }

  getColorName(baseName: string, shade: number): string {
    return `${baseName}-${shade}`
  }

  /**
   * Retourne un objet de style pour le binding [style] d'Angular,
   * utilisant une variable CSS pour la couleur de fond du bloc.
   */
  getCardBackgroundStyles(baseName: string, shade: number): Record<string, string> {
    const cssVarName = `--p-${baseName}-${shade}`
    return {
      'background-color': `var(${cssVarName})`,
    }
  }
}
