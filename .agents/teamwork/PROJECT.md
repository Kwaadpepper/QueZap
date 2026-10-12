# Project: QueZap Comprehensive Audit

## Architecture
- Scope: Read-Only Audit of QueZap (Spring Boot 3.5.7 + Angular 21 monorepo)
- Artifact directory: `.audit/`
  - `.audit/screenshots/`: Visual captures of host (1920x1080) and mobile player (390x844)
  - `.audit/BLACKBOARD.md`: 4-expert deliberation across 3 rounds (Diagnostic, Confrontation, Synthesis)
  - `.audit/ACTION_PLAN.md`: Prioritized action plan (P0, P1, P2) with real verified files and acceptance criteria
- Rules: NO source code modifications allowed (`git status` must show 0 changes outside `.audit/`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | R1 Visual Coverage Completion | Ensure all required live screens are captured and indexed in `.audit/screenshots/INDEX.txt` or absence justified | M1 | ORIGINAL_REQUEST.md §R1 |
| 2 | Git Status Hygiene | Inspect git status, revert any unauthorized modifications outside `.audit/` | M1 | ORIGINAL_REQUEST.md §R4 |
| 3 | R2 Blackboard Review | Verify 4 experts, 3 rounds, 4 sections, citations, and consensus in `.audit/BLACKBOARD.md` | M2 | ORIGINAL_REQUEST.md §R2 |
| 4 | R3 Master Action Plan | Author `.audit/ACTION_PLAN.md` with P0/P1/P2, assigned roles, verified codebase files, and testable criteria | M3 | ORIGINAL_REQUEST.md §R3 |
| 5 | R4 Final Integrity Audit | Verify git status is completely clean outside `.audit/` and all acceptance criteria pass | M4 | ORIGINAL_REQUEST.md §R4 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | Visual Coverage & Git Inspection | Verify screenshots, update INDEX.txt, check git status | none | DONE |
| 2 | Blackboard Deliberation Validation | Verify BLACKBOARD.md against criteria (4 sections, citations) | M1 | DONE |
| 3 | Action Plan Authoring & Verification | Write ACTION_PLAN.md, verify cited files exist in codebase | M2 | DONE |
| 4 | Final Integrity & Audit Gate | Run forensic check on git status and criteria | M3 | DONE |

## Code Layout & Write Boundaries
- Strictly restricted to `.audit/` for deliverables and `.agents/teamwork/` for agent metadata.
- NO file modifications permitted in `back/`, `front/`, etc.
