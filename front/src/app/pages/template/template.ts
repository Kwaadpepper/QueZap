import { JsonPipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'

import { AccordionModule } from '@openng/optimus-ui/accordion'
import { MessageService } from '@openng/optimus-ui/api'
import { AutoCompleteModule } from '@openng/optimus-ui/autocomplete'
import { BadgeModule } from '@openng/optimus-ui/badge'
import { ButtonModule } from '@openng/optimus-ui/button'
import { CardModule } from '@openng/optimus-ui/card'
import { CheckboxModule } from '@openng/optimus-ui/checkbox'
import { DialogModule } from '@openng/optimus-ui/dialog'
import { DividerModule } from '@openng/optimus-ui/divider'
import { FieldsetModule } from '@openng/optimus-ui/fieldset'
import { ImageModule } from '@openng/optimus-ui/image'
import { InputNumberModule } from '@openng/optimus-ui/inputnumber'
import { InputTextModule } from '@openng/optimus-ui/inputtext'
import { MessageModule } from '@openng/optimus-ui/message'
import { PanelModule } from '@openng/optimus-ui/panel'
import { ProgressBarModule } from '@openng/optimus-ui/progressbar'
import { RadioButtonModule } from '@openng/optimus-ui/radiobutton'
import { RatingModule } from '@openng/optimus-ui/rating'
import { SelectModule } from '@openng/optimus-ui/select'
import { SliderModule } from '@openng/optimus-ui/slider'
import { TabsModule } from '@openng/optimus-ui/tabs'
import { TagModule } from '@openng/optimus-ui/tag'
import { ToastModule } from '@openng/optimus-ui/toast'
import { ToggleSwitchModule } from '@openng/optimus-ui/toggleswitch'

import { IconFacade } from '@quezap/shared/components/icon/icon-facade'

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
    DialogModule,
    DividerModule,
    FieldsetModule,
    FormsModule,
    ImageModule,
    InputNumberModule,
    InputTextModule,
    JsonPipe,
    MessageModule,
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
    IconFacade,
  ],
  providers: [MessageService],
  templateUrl: './template.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
    return { 'background-color': `var(${cssVarName})` }
  }
}
