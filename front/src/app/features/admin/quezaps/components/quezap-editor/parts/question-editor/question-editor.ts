import { ChangeDetectionStrategy, Component } from '@angular/core'

@Component({
  selector: 'quizz-question-editor',
  imports: [],
  templateUrl: './question-editor.html',
  styles: ':host { display: block; height: 100%; width: 100%; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditor {

}
