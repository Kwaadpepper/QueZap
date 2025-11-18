import { UUID } from '../types'

import { Theme } from './theme'

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

export interface Question {
  readonly id: QuestionId
  readonly value: string
  readonly picture?: PictureUrl
  readonly type: QuestionType
  readonly limit?: Timer
  readonly theme: Theme
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
