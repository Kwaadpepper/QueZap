import { UUID } from '../types'

export interface Timer {
  readonly seconds: number
}

export type PictureUrl = string & {
  readonly __type: 'PictureUrl'
}

export type QuestionId = UUID & {
  readonly __type: 'QuestionId'
}

export enum QuestionType {
  Boolean = 'BOOLEAN',
  Binary = 'BINARY',
  Quizz = 'QUIZZ',
}

export const QuestionTypeFrom = (enumObj: QuestionType) => ({
  getNewWithAnswers: (): QuestionWithAnswersAndResponses => {
    switch (enumObj) {
      case QuestionType.Boolean:
        return {
          id: '' as QuestionId,
          value: '',
          type: QuestionType.Boolean,
          answers: [
            getNewAnswerWithResponse(0),
            getNewAnswerWithResponse(1),
          ],
        }
      case QuestionType.Binary:
        return {
          id: '' as QuestionId,
          value: '',
          type: QuestionType.Binary,
          answers: [
            getNewAnswerWithResponse(0),
            getNewAnswerWithResponse(1),
          ],
        }
      case QuestionType.Quizz:
        return {
          id: '' as QuestionId,
          value: '',
          type: QuestionType.Quizz,
          answers: [
            getNewAnswerWithResponse(0),
            getNewAnswerWithResponse(1),
            getNewAnswerWithResponse(2),
            getNewAnswerWithResponse(3),
          ],
        }
      default:
        throw new Error(`Unhandled QuestionType: ${enumObj}`)
    }
  },
  toString: () => {
    switch (enumObj) {
      case QuestionType.Boolean:
        return 'Vrai / Faux'
      case QuestionType.Binary:
        return 'Deux Options'
      case QuestionType.Quizz:
        return 'Quizz'
      default:
        throw new Error(`Unhandled QuestionType: ${enumObj}`)
    }
  },
})

export function getNewAnswerWithResponse(index: number): AnswerWithResponse {
  if (index < 0) {
    throw new Error('Answer index must be non-negative')
  }

  return {
    index,
    points: 0,
    isCorrect: false,
    picture: undefined,
    value: undefined,
  }
}

export interface Question {
  readonly id: QuestionId
  readonly value: string
  readonly picture?: PictureUrl
  readonly type: QuestionType
  readonly limit?: Timer
}

export interface BooleanQuestion extends Question {
  readonly type: QuestionType.Boolean
}

export interface BinaryQuestion extends Question {
  readonly type: QuestionType.Binary
}

export interface QuizzQuestion extends Question {
  readonly type: QuestionType.Quizz
}

export type MixedQuestion = BooleanQuestion | BinaryQuestion | QuizzQuestion

export interface Answer {
  readonly index: number
  readonly points: number
  readonly value?: string
  readonly picture?: PictureUrl
}

export interface AnswerWithResponse extends Answer {
  readonly isCorrect: boolean
}

export interface QuestionWithAnswers extends Question {
  readonly answers: Answer[]
}

export interface QuestionWithAnswersAndResponses extends Question {
  readonly answers: AnswerWithResponse[]
}

export interface QuestionWithPicture extends Omit<Question, 'picture'> {
  readonly picture: PictureUrl
}

export interface QuestionWithTimer extends Question {
  readonly limit: Timer
}

export interface AnswerWithPicture extends Omit<Answer, 'picture'> {
  readonly picture: PictureUrl
}

export function isQuestionWithTimer(
  question: Question,
): question is QuestionWithTimer {
  return !!question.limit
}

export function isQuestionWithPicture(
  question: Question,
): question is QuestionWithPicture {
  return !!question.picture
}

export function isAnswerWithPicture(
  answer: Answer,
): answer is AnswerWithPicture {
  return !!answer.picture
}

export function isQuizzQuestion(
  question: Question,
): question is QuizzQuestion {
  return question.type === QuestionType.Quizz
}

export function isBooleanQuestion(
  question: Question,
): question is BooleanQuestion {
  return question.type === QuestionType.Boolean
}

export function isBinaryQuestion(
  question: Question,
): question is BinaryQuestion {
  return question.type === QuestionType.Binary
}
