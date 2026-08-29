# XMLInstanceEditor Agent Guide

## Purpose
This repository builds a schema-driven XML instance editor:
- **Frontend:** Svelte UI for uploading XSD, selecting/creating root, editing instance values, and previewing XML.
- **Backend:** Quarkus REST API that serves the frontend, parses schemas, validates XML, and persists edited instances.

## Repository Structure
- `backend/` — Quarkus app, REST resources, Quinoa integration, frontend asset serving.
- `api/` — framework-independent services and storage abstractions.
- `schema-core/` — XSD translation and schema intermediate model.
- `xml-generator/` — XML node model and serializer support.
- `frontend/` — SvelteKit frontend.

## Key Runtime Paths
- Frontend app: `/` and `/index.html`
- API base path: `/api`
- Schema parse: `POST /api/schema/parse`
- XML validation: `POST /api/instance/validate`
- Instance persistence:
  - `POST /api/instances`
  - `GET /api/instances`
  - `GET /api/instances/{id}`
  - `DELETE /api/instances/{id}`

## Local Development
Prerequisites:
- Java 17
- Node.js + npm

Commands:
1. Backend dev mode (serves frontend via Quinoa):  
   `./gradlew :backend:quarkusDev`
2. Frontend checks:  
   `npm --prefix frontend run check`
3. Full test suite:  
   `./gradlew test`

Notes:
- Gradle wrapper is pinned to **8.10.2** for Quarkus compatibility.
- Backend property `xmlinstanceeditor.storage.root-dir` controls local persisted instance storage (default: `backend/build/instances`).

## Architecture Conventions
- Keep Quarkus/JAX-RS code in `backend` only.
- Keep reusable logic framework-free in `api`/`schema-core`/`xml-generator`.
- Storage is accessed through `InstanceStore<T>` so alternate backends (for example BaseX) can be added without changing callers.
- For schemas without global roots, frontend synthetic roots should serialize using `xsi:type`.

## Change Guidance
- Make focused changes and preserve module boundaries.
- Prefer extending existing models/services over duplicating logic.
- Update both frontend types and backend models together when API shapes change.
- Keep API routes under `/api` to avoid conflicts with frontend routing.
