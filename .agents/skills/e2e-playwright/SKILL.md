---
name: e2e-playwright
description: >-
  Guide and best practices for multi-client end-to-end browser testing and classroom simulation
  for QueZap (1 teacher host screen + multiple student player tabs interacting over SSE).
---

# E2E Playwright Testing & Classroom Simulation Guide

This skill guides the implementation and execution of automated end-to-end tests simulating a real classroom quiz session.

## Architecture of a QueZap E2E Test Scenario

A complete classroom simulation requires multiple isolated browser contexts running concurrently:
1. **Teacher Context (Host / Projector)**:
   - Navigates to `/admin/sessions/{id}/present`.
   - Displays lobby with PIN and QR code.
   - Observes participants joining in real-time.
   - Starts the session, navigates between questions, shows answers, and displays the final podium.
2. **Student Contexts (Players 1..N)**:
   - Navigate to `/quizz?code={PIN}` on simulated mobile viewports (e.g. iPhone 14 / Pixel 7).
   - Enter their nicknames and avatars.
   - Wait in lobby.
   - Receive the question via Server-Sent Events (SSE).
   - Submit answers with varying speeds (measuring scoring algorithm).
   - Receive instantaneous feedback and ranking.

## Running Headless & Multi-Tab Assertions

- Verify Server-Sent Events (SSE) stream connects and processes events without timeouts.
- Assert question countdown synchronization across all contexts.
- Validate scoring formula: faster answers receive higher points.
- Verify session recovery when refreshing a student tab (`sessionStorage` participation token persistence).

