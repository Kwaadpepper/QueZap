import { Component, computed, effect, input } from '@angular/core'

import { type AnimationProp, FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import type {
  FaSymbol,
  FlipProp, IconDefinition, IconProp,
  PullProp, RotateProp, SizeProp,
  Transform,
} from '@fortawesome/fontawesome-svg-core'
import { library } from '@fortawesome/fontawesome-svg-core'
import {
  faAddressCard,
  faArrowLeft,
  faArrowRight,
  faArrowUp,
  faBars,
  faBug,
  faCheck,
  faCheckCircle,
  faCheckSquare,
  faCircleExclamation,
  faCog,
  faCopy,
  faDownLeftAndUpRightToCenter,
  faHome,
  faInfoCircle,
  faLeaf,
  faPencil,
  faPlayCircle,
  faPlus,
  faQuestion,
  faQuestionCircle,
  faSignOut,
  faTimes,
  faTrash,
  faTriangleExclamation,
  faUpRightAndDownLeftFromCenter,
} from '@fortawesome/free-solid-svg-icons'

export const ICON_MAP = {

  'bars': faBars,
  'debug': faBug,
  'cog': faCog,
  'copy': faCopy,
  'trash': faTrash,
  'plus': faPlus,
  'pencil': faPencil,
  'sign-out': faSignOut,

  'bigger': faUpRightAndDownLeftFromCenter,
  'smaller': faDownLeftAndUpRightToCenter,

  'arrow-up': faArrowUp,
  'arrow-right': faArrowRight,
  'arrow-left': faArrowLeft,
  'question': faQuestion,
  'question-circle': faQuestionCircle,
  'check-circle': faCheckCircle,
  'check-square': faCheckSquare,
  'check': faCheck,

  'play-circle': faPlayCircle,
  'home': faHome,
  'leaf': faLeaf,

  // Semantic icons
  'cancel': faTimes,
  'confirm': faCheck,
  'exit': faTimes,
  'error': faCircleExclamation,
  'info': faInfoCircle,
  'warn': faTriangleExclamation,
}

export type IconName = keyof typeof ICON_MAP
export type IconSeverity
  = | 'success'
    | 'info'
    | 'warn'
    | 'danger'
    | 'help'
    | 'primary'
    | 'secondary'
    | 'contrast'

@Component({
  selector: 'quizz-icon',
  standalone: true,
  imports: [
    FontAwesomeModule,
  ],
  template: `
    @if(icon) {<fa-icon
      aria-hidden="true"
      [icon]="icon"
      [title]="title()"
      [animation]="animation()"
      [mask]="mask()"
      [flip]="flip()"
      [size]="size()"
      [pull]="pull()"
      [border]="border()"
      [inverse]="inverse()"
      [symbol]="symbol()"
      [rotate]="rotate()"
      [fixedWidth]="fixedWidth()"
      [transform]="transform()"
      [class]="class() + ' ' + (severtyText() || '')"
    />}
  `,
  styles: ':host { display: inline-block; }',
})
export class IconFacade {
  private readonly severityBackgroundMap = new Map<IconSeverity, string>([
    ['success', 'text-green-100 bg-green-700 rounded-full p-1'],
    ['info', 'text-blue-100 bg-blue-700 rounded-full p-1'],
    ['warn', 'text-yellow-100 bg-yellow-700 rounded-full p-1'],
    ['danger', 'text-red-100 bg-red-700 rounded-full p-1'],
    ['help', 'text-purple-100 bg-purple-700 rounded-full p-1'],
    ['primary', 'text-indigo-100 bg-indigo-700 rounded-full p-1'],
    ['secondary', 'text-gray-100 bg-gray-700 rounded-full p-1'],
    ['contrast', 'text-black bg-white rounded-full p-1'],
  ])

  readonly name = input.required<IconName>()

  readonly title = input<string | undefined>(undefined)
  readonly animation = input<AnimationProp | undefined>(undefined)
  readonly mask = input<IconProp | undefined>(undefined)
  readonly flip = input<FlipProp | undefined>(undefined)
  readonly size = input<SizeProp | undefined>(undefined)
  readonly pull = input<PullProp | undefined>(undefined)
  readonly border = input<boolean | undefined>(undefined)
  readonly inverse = input<boolean | undefined>(undefined)
  readonly symbol = input<FaSymbol | undefined>(undefined)
  readonly rotate = input<RotateProp | undefined>(undefined)
  readonly fixedWidth = input<boolean | undefined>(undefined)
  readonly transform = input<string | Transform | undefined>(undefined)

  readonly class = input<string | undefined>(undefined)

  readonly severity = input<IconSeverity | undefined>(undefined)
  readonly severtyText = computed(() => {
    const severity = this.severity()
    if (!severity) {
      return undefined
    }
    return this.severityBackgroundMap.get(severity)
  })

  protected icon: IconDefinition | undefined

  constructor() {
    library.add(
      faTriangleExclamation,
      faAddressCard,
    )
    effect(() => {
      this.icon = ICON_MAP[this.name()]

      if (!this.icon) {
        console.error(`Icon not found: '${this.name}'. Please add it to ICON_MAP.`)
        this.icon = faTriangleExclamation
      }
    })
  }
}
