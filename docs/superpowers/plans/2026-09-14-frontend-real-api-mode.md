# Frontend Real API Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow developers to run the existing student journey against the Spring API with one command, while preserving the mock as the default development mode.

**Architecture:** Add a dedicated Angular development configuration that replaces the default environment with a real API environment. The existing `ApiClient`, auth interceptors, student services and routes remain the single runtime path; only the mock interceptor's existing feature flag changes. Add a focused configuration test and document the required backend and database setup.

**Tech Stack:** Angular 22, TypeScript, Vitest, Angular CLI, Spring Boot backend, PostgreSQL.

**Spec:** `docs/frontend/07-qualidade-entrega.md`, `frontend/README.md`, and the approved contract in `docs/frontend-integration-spec.md`.

## Global Constraints

- Keep `frontend/src/environments/environment.ts` using `useMockApi: true` for the default development command.
- The real mode must use the `/api/v1` base path and the existing `proxy.conf.json` forwarding to `http://localhost:8080`.
- Do not add default lessons, shared question seeds, or frontend calculations for server-owned progress data.
- Preserve the existing auth interceptor, refresh behavior, error handling, and student route structure.
- Run commands from `frontend/` and use the npm scripts backed by the lockfile.

### Task 1: Specify the real environment with a failing test

**Files:**
- Create: `frontend/src/environments/environment.real.spec.ts`

**Interfaces:**
- Produces assertions for the real development environment consumed by the Angular build configuration.

- [x] **Step 1: Write the failing test**

```ts
import { environment } from './environment.real';

describe('environment real', () => {
  it('desativa o mock e mantém o contrato da API', () => {
    expect(environment.production).toBe(false);
    expect(environment.apiBaseUrl).toBe('/api/v1');
    expect(environment.useMockApi).toBe(false);
    expect(environment.presentationTimeZone).toBe('America/Sao_Paulo');
  });
});
```

- [x] **Step 2: Run the test to verify it fails for the missing environment**

Run: `npm run test:ci -- --include=src/environments/environment.real.spec.ts`

Expected: FAIL because `src/environments/environment.real.ts` does not exist yet.

### Task 2: Add the real API Angular configuration

**Files:**
- Create: `frontend/src/environments/environment.real.ts`
- Modify: `frontend/angular.json` in the build and serve configurations
- Modify: `frontend/package.json` scripts
- Test: `frontend/src/environments/environment.real.spec.ts`

**Interfaces:**
- `environment.real.ts` exports the same typed shape as the default environment with `useMockApi: false`.
- `npm run start:real` serves the app using the `real` serve configuration.
- `npm run build:real` builds using the real API environment replacement.

- [x] **Step 1: Write the minimal real environment**

```ts
export const environment = {
  production: false,
  apiBaseUrl: '/api/v1',
  useMockApi: false,
  presentationTimeZone: 'America/Sao_Paulo',
} as const;
```

- [x] **Step 2: Run the focused test to verify it passes**

Run: `npm run test:ci -- --include=src/environments/environment.real.spec.ts`

Expected: PASS for the environment contract test.

- [x] **Step 3: Wire the build replacement**

Add a `development-real` build configuration in `frontend/angular.json` with the same development compiler settings and this replacement:

```json
"fileReplacements": [
  {
    "replace": "src/environments/environment.ts",
    "with": "src/environments/environment.real.ts"
  }
]
```

Add a `real` serve configuration targeting `frontend:build:development-real`.

- [x] **Step 4: Add npm entry points**

Add these scripts to `frontend/package.json`:

```json
"start:real": "ng serve --configuration real",
"build:real": "ng build --configuration development-real"
```

- [x] **Step 5: Build the real configuration**

Run: `npm run build:real`

Expected: Angular builds successfully and uses the real environment replacement.

### Task 3: Document and verify the local real journey entry point

**Files:**
- Modify: `frontend/README.md` near the execution and backend sections

**Interfaces:**
- A developer can start PostgreSQL and the backend, run `npm run start:real`, and understand that the account, room and lesson data must exist in the backend database.

- [x] **Step 1: Document both modes**

Keep `npm start` as the mock-backed command and add:

```text
npm run start:real
```

Explain that this command uses `/api/v1` through `proxy.conf.json`, requires the backend on port 8080 and does not create demo student, room or lesson data automatically.

- [x] **Step 2: Run the frontend quality gate**

Run: `npm run verify`

Expected: lint passes, architectural rules report no violations, all tests pass, and the production build completes.

- [x] **Step 3: Review the final diff**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; only the planned environment, Angular configuration, package script, README and previously preserved local validation changes are present.
