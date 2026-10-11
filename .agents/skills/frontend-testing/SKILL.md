---
name: frontend-testing
description: >-
  Procedures, best practices, and fast commands for testing the QueZap Angular 21 frontend
  with Jest, NgRx Signal Stores, Zod, ESLint, and Stylelint.
---

# Frontend Testing Guide (QueZap)

This skill provides guidelines and fast commands to test and validate the Angular 21 frontend.

## ⚡ Fast Feedback Loop (Targeted Jest Testing)

Do not run the entire test suite when iterating on a component or service. Run targeted Jest tests:

```bash
# Test a single component spec file
cd front && bun run test -- src/app/features/admin/themes/components/theme-card/theme-card.spec.ts

# Run tests matching a regex pattern
cd front && bun run test -- quizz

# Run in watch mode during development
cd front && bun run test:watch -- src/app/features/admin/quezaps/

# Generate coverage report
cd front && bun run test:coverage
```

---

## 🧹 Linters & Format Checks

Always verify linting before concluding frontend changes:

```bash
cd front

# Run all linters (TypeScript + CSS)
bun run lint

# Lint TypeScript / Angular templates with auto-fix
bun run lint:ts:fix

# Lint CSS / Tailwind CSS with auto-fix
bun run lint:css:fix
```

---

## 🧩 Angular 21 & Testing Conventions

1. **Standalone Components & Signal Inputs**:
   - Use `TestBed.configureTestingModule({ imports: [MyComponent] })`.
   - Set signal inputs using `fixture.componentRef.setInput('propName', value)`.
   - Call `fixture.detectChanges()` after setting inputs.

2. **Strict Typing (Zero `any`)**:
   - Never use `any` or `$any()`. Use strong types or Zod schemas.
   - For mock services, implement the interface (e.g. `implements ThemeService`) or type with `jest.Mocked<ThemeService>`.

3. **RxJS & Memory Management**:
   - Verify that subscriptions in components are destroyed with `takeUntilDestroyed(destroyRef)`.
   - In tests, destroy the fixture (`fixture.destroy()`) and ensure no lingering subscriptions or timers remain.

4. **Testing Signal Stores (`@ngrx/signals`)**:
   - Inject the store using `TestBed.inject(MyStore)`.
   - Assert reactive signals directly: `expect(store.items()).toEqual(...)`.
   - Test methods that mutate state and assert resulting signal values.

---

## 🔄 API Synchronization

If backend endpoints were updated:
```bash
# In project root: export new openapi.json
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"

# In front directory: regenerate TypeScript client
cd front && bun run codegen
```
