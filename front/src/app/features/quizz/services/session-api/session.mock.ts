import { Session, SessionCode, SessionId } from '@quezap/domain/models'

export const MOCK_SESSIONS: Session[] = [
  {
    id: '019a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'A2B3C4' as SessionCode,
    name: 'Les arbres caduques',
  },
  {
    id: '129a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'D5E6F7' as SessionCode,
    name: 'Les végetaux aquatiques',
  },
  {
    id: '239a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'G8H9I0' as SessionCode,
    name: 'Les fleurs sauvages',
  },
  {
    id: '339a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'J1K2L3' as SessionCode,
    name: 'Les plantes médicinales',
  },
  {
    id: '439a87fd-3713-7f62-9081-bf7cb1542208' as SessionId,
    code: 'M4N5O6' as SessionCode,
    name: 'Les arbres fruitiers',
  },
]
