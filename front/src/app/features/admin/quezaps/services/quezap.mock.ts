import {
  QuestionId, QuestionType, QuezapId, QuezapWithQuestionsAndAnswers, QuezapWithTheme, ThemeId,
} from '@quezap/domain/models'

export const MOCK_QUEZAPS: (QuezapWithQuestionsAndAnswers & QuezapWithTheme)[] = [
  {
    id: '019a87fd-3713-7f62-9081-bf7cb1542203' as QuezapId,
    title: 'Plantes caducs ou persistants ?',
    description: 'Découvrez si vous connaissez les plantes à feuilles persistantes et caduques.',
    theme: {
      id: '019a87fd-3713-7f62-9081-bf7cb1542203' as ThemeId,
      name: 'Botanique',
    },
    questionWithAnswersAndResponses: [
      {
        id: '219a87fd-3713-7f62-9081-bf7cb1542203' as QuestionId,
        value: 'Laquelle de ces plantes est persistante ?',
        type: QuestionType.Quizz,
        answers: [
          {
            index: 0,
            value: 'Le chêne',
            isCorrect: true,
            points: 10,
          },
          {
            index: 1,
            value: 'L\'érable',
            isCorrect: false,
            points: 0,
          },
          {
            index: 2,
            value: 'Le bouleau',
            isCorrect: false,
            points: 0,
          },
          {
            index: 3,
            value: 'Le peuplier',
            isCorrect: false,
            points: 0,
          },
        ],
      },
      {
        id: '319a87fd-3713-7f62-9081-bf7cb1542203' as QuestionId,
        value: 'Parmi les plantes suivantes, lesquelles sont caduques ?',
        type: QuestionType.Binary,
        answers: [
          {
            index: 0,
            value: 'Le hêtre',
            isCorrect: true,
            points: 10,
          },
          {
            index: 1,
            value: 'Le laurier-rose',
            isCorrect: false,
            points: 0,
          },
        ],
      },
      {
        id: '419a87fd-3713-7f62-9081-bf7cb1542203' as QuestionId,
        value: 'Les conifères sont-ils généralement persistants ?',
        type: QuestionType.Boolean,
        answers: [
          {
            index: 0,
            value: 'Vrai',
            isCorrect: true,
            points: 10,
          },
          {
            index: 1,
            value: 'Faux',
            isCorrect: false,
            points: 0,
          },
        ],
      },
    ],
  },
  {
    id: '119a87fd-3713-7f62-9081-bf7cb1542203' as QuezapId,
    title: 'Les suculentes',
    description: 'Testez vos connaissances sur les plantes grasses et suculentes.',
    theme: {
      id: '119a87fd-3713-7f62-9081-bf7cb1542203' as ThemeId,
      name: 'Botanique',
    },
    questionWithAnswersAndResponses: [
      {
        id: '519a87fd-3713-7f62-9081-bf7cb1542203' as QuestionId,
        value: 'Quelle est la principale caractéristique des plantes succulentes ?',
        type: QuestionType.Quizz,
        answers: [
          {
            index: 0,
            value: 'Elles ont des feuilles épaisses pour stocker l\'eau',
            isCorrect: true,
            points: 10,
          },
          {
            index: 1,
            value: 'Elles ont des racines très profondes',
            isCorrect: false,
            points: 0,
          },
          {
            index: 2,
            value: 'Elles fleurissent toute l\'année',
            isCorrect: false,
            points: 0,
          },
          {
            index: 3,
            value: 'Elles poussent uniquement dans les climats froids',
            isCorrect: false,
            points: 0,
          },
        ],
      },
      {
        id: '619a87fd-3713-7f62-9081-bf7cb1542203' as QuestionId,
        value: 'Les cactus sont-ils des plantes succulentes ?',
        type: QuestionType.Boolean,
        answers: [
          {
            index: 0,
            value: 'Vrai',
            isCorrect: true,
            points: 10,
          },
          {
            index: 1,
            value: 'Faux',
            isCorrect: false,
            points: 0,
          },
        ],
      },
    ],
  },
]
