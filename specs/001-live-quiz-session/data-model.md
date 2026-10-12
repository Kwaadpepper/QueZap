# Data Model: Diffusion et animation de quiz interactif en classe

**Feature**: Diffusion de questions et animation de session en classe (`specs/001-live-quiz-session`)  
**Date**: 2026-10-12

## 1. Modèle Métier Domaine (`modules:domain`)

### 1.1 Entité & Agrégat `Session`

```text
Session (AggregateRoot<SessionId>)
├── id: SessionId
├── name: SessionName
├── number: SessionNumber
├── author: UserId
├── questionSlides: Set<QuestionSlide>
├── participants: Set<Participant>
├── answers: Set<QuestionAnswer>
├── currentSlideIndex: Integer
├── startedAt: TimelinePoint?
├── endedAt: TimelinePoint?
│
├── [NOUVEAU] progressionMode: ProgressionMode (MANUAL | AUTOMATIC)
├── [NOUVEAU] isPaused: boolean
├── [NOUVEAU] explanationDurationSeconds: Integer (default 15)
└── [NOUVEAU] mobileCorrectionEnabled: boolean (default false)
```

#### Événements de Domaine (`com.quezap.domain.events.sessions`)
- `SessionPaused(SessionId sessionId, TimelinePoint pausedAt)`
- `SessionResumed(SessionId sessionId, TimelinePoint resumedAt)`
- `SessionEnded(SessionId sessionId, TimelinePoint endedAt)` (enrichi des métriques collectives)

---

## 2. DTOs & Contrats d'API REST (`quezap-api`)

### 2.1 Requêtes et Réponses REST

#### Contrôle de la pause : `POST /apiv1/sessions/{id}/pause`
```json
// Request Body
{
  "paused": true
}

// Response: 200 OK
{
  "sessionId": "0192e345-6789-7abc-def0-123456789abc",
  "isPaused": true
}
```

#### Configuration du mode et de la durée d'explication : `PUT /apiv1/sessions/{id}/settings`
```json
// Request Body
{
  "progressionMode": "AUTOMATIC",
  "explanationDurationSeconds": 15,
  "mobileCorrectionEnabled": false
}

// Response: 200 OK
{
  "progressionMode": "AUTOMATIC",
  "explanationDurationSeconds": 15,
  "mobileCorrectionEnabled": false
}
```

---

## 3. Événements SSE (`SessionSseService` -> Front)

### 3.1 Événements de Pause & Reprise
- **Nom d'événement** : `SESSION_PAUSE_STATUS`
```json
{
  "type": "SESSION_PAUSE_STATUS",
  "sequence": 42,
  "sessionNumber": 101,
  "sessionId": "0192e345-6789-7abc-def0-123456789abc",
  "payload": {
    "isPaused": true,
    "timestamp": "2026-10-12T19:40:00Z"
  }
}
```

### 3.2 Événement de Révélation de Question (`QUESTION_REVEAL`)
```json
{
  "type": "QUESTION_REVEAL",
  "payload": {
    "slideIndex": 1,
    "correctAnswerIndices": [0],
    "explanation": "La photosynthèse convertit l'énergie lumineuse en énergie chimique.",
    "answersStats": [
      { "answerIndex": 0, "count": 18, "percentage": 90.0 },
      { "answerIndex": 1, "count": 2, "percentage": 10.0 }
    ],
    "mobileCorrectionAllowed": false
  }
}
```

### 3.3 Événement de Fin de Session Enrichi (`SESSION_ENDED`)
```json
{
  "type": "SESSION_ENDED",
  "payload": {
    "endedAt": "2026-10-12T19:55:00Z",
    "metrics": {
      "totalParticipants": 25,
      "averageScore": 7450,
      "globalSuccessRate": 82.5
    },
    "top5": [
      { "rank": 1, "participantName": "Léa", "score": 9800, "correctCount": 10 },
      { "rank": 2, "participantName": "Mehdi", "score": 9200, "correctCount": 9 },
      { "rank": 3, "participantName": "Chloé", "score": 8900, "correctCount": 9 },
      { "rank": 4, "participantName": "Alexandre", "score": 8500, "correctCount": 8 },
      { "rank": 5, "participantName": "Inès", "score": 8200, "correctCount": 8 }
    ],
    "finalLeaderboard": [
      // Liste ordonnée intégrale des 25 participants
    ]
  }
}
```

---

## 4. Modèle Frontend (`front/src/app`)

### 4.1 Modèle de l'état Élève (`ActiveSessionStore`)
- `isSessionPaused`: Signal booléen indiquant la mise en pause explications.
- `mySubmittedChoice`: Stocke l'index de la réponse validée et le libellé pour l'affichage de maintien statique.
- `finalMetrics`: Stocke `{ totalParticipants, averageScore, globalSuccessRate }`.
- `fullLeaderboard`: Signal contenant la liste ordonnée complète après la clôture.

### 4.2 Modèle de Présentation Enseignant (`SessionPresenter`)
- `progressionMode`: Signal `'MANUAL' | 'AUTOMATIC'`.
- `explanationTimer`: Signal du compte à rebours d'explication (initialisé à `explanationDurationSeconds`, défaut 15s).
- `isPaused`: Signal booléen de pause.
- `top5`: Computed dérivant les 5 premiers pour la disposition compacte sans scroll.
- `classMetrics`: Signal `{ averageScore, globalSuccessRate }`.
