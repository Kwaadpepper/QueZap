import { ChangeDetectionStrategy, Component, input } from '@angular/core'

import { Image } from 'primeng/image'

import { PictureUrl } from '@quezap/domain/models'

@Component({
  selector: 'quizz-picture',
  imports: [Image],
  templateUrl: './picture.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Picture {
  readonly pictureUrl = input.required<PictureUrl>()
  readonly altText = input<string>()
}
