# Hegemony Assistant MVP (Local-First Monorepo)

Local-first monorepo with a rules-validating backend and modern web frontend.

## Monorepo layout

- `backend` - Java 21 + Spring Boot + Gradle
- `frontend` - React + TypeScript + Vite + Tailwind + shadcn-style components

## Windows local run, no WSL

From repo root, run:

```bat
start-local.bat
```

The launcher checks Java 21, Node.js LTS/npm, installs frontend packages when needed, creates `data/saves`, and opens:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/game`

If Java or Node.js is missing on a Windows 10/11 machine with `winget`, run:

```bat
start-local.bat -InstallMissing
```

Stop the app with `Ctrl+C` in the backend/frontend windows, or close those windows.

## Docker Compose run

Run everything with one command from repo root:

```bash
docker compose up --build --force-recreate
```

Then open:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/game`

JSON saves are persisted in `./data/saves` on host.

Stop stack:

```bash
docker compose down
```

## Product scope in this MVP

This is a narrow vertical slice, not the full Hegemony ruleset:

- backend owns authoritative game state
- user submits structured move commands via guided action composer
- rules engine validates and applies legal commands
- command application emits domain events and writes readable event log entries
- bot chooses only from engine-generated legal moves
- bot can auto-play one BOT turn or continue until next HUMAN decision point
- action preview computes projected deltas without mutating game state
- per-class control mode (`HUMAN` / `BOT`) is configurable and persisted in save/load
- save/load uses JSON files only

Implemented demonstrative action subset:

- `PROPOSE_BILL`
- `ASSIGN_WORKERS`
- `BUY_GOODS_AND_SERVICES`
- `CONSUME_HEALTHCARE`
- `CONSUME_EDUCATION`
- `CONSUME_LUXURY`
- supported lifecycle/voting/production/scoring transition commands

## Backend run

```bash
cd backend
./gradlew bootRun
```

Backend API runs on `http://localhost:8080`.

## Frontend run

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` and proxies `/api` to backend.

## Backend tests

```bash
cd backend
./gradlew test
```

Tests include:

- domain rules validation
- state transition checks
- multi-step scenario sequence
- bot legal-move guarantee

## REST API

- `GET /api/game`
- `POST /api/game/reset`
- `POST /api/game/setup`
- `POST /api/game/command`
- `POST /api/game/preview`
- `GET /api/game/legal-moves`
- `POST /api/game/bot-move`
- `POST /api/game/play-bot-turn`
- `POST /api/game/play-bot-until-human`
- `POST /api/game/save`
- `POST /api/game/load`

## Cards extension path

Card definitions are loaded from JSON:

- `backend/src/main/resources/cards/sample-cards.json`

Current card model supports:

- declarative effect list (`GAIN_MONEY`, `GAIN_GOODS`, `ADJUST_TAXATION`)
- optional `customResolver` key reserved for future Java edge-case resolvers
- card-ready abstractions for enterprise catalogs and simple-automa card catalogs
- explicit readiness flags when card datasets are not installed (heuristic fallback mode)

This keeps the first iteration simple while providing a clean extension point for richer card logic later.
