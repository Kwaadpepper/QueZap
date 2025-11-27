import { UUID } from '../types'

import { Question, QuestionWithAnswersAndResponses } from './questions'
import { Theme } from './theme'

export type QuezapId = UUID & {
  readonly __type: 'Quezap'
}

export interface Quezap {
  readonly id: QuezapId
  readonly title: string
  readonly description: string
}

export interface QuezapWithTheme extends Quezap {
  readonly theme: Theme
}

export interface QuezapWithQuestions extends Quezap {
  readonly questions: Question[]
}

export interface QuezapWithQuestionsAndAnswers extends Quezap {
  readonly questionWithAnswersAndResponses: QuestionWithAnswersAndResponses[]
}

export function emptyRawQuezap(): Omit<QuezapWithQuestionsAndAnswers, 'id'> {
  return {
    title: '',
    description: '',
    questionWithAnswersAndResponses: [],
  }
}
