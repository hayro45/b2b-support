# MVP Implementation Plan (Execution-Oriented)

## Architecture decisions

1. Monorepo: backend, frontend, infra, docs
2. Single Ubuntu host deployment with Docker Compose
3. Host Nginx reverse proxy for single subdomain + path routing
4. PostgreSQL on same host for cost control
5. MVP excludes file upload
6. Demo seed data is auto-loaded by Flyway migrations
7. Initial user target is 5-6 users

## Finalized deployment decisions

1. API routing strategy: same subdomain under `/api`
- Frontend URL: `https://support.hayrettindal.com`
- API URL: `https://support.hayrettindal.com/api`
- Why: no CORS complexity, simpler certificate management, easier operations on one low-resource server.

2. Database strategy: PostgreSQL in Docker on same Ubuntu host
- Why: zero extra vendor cost, enough for 5-6 users, simple backup and restore.
- Trade-off: lower resilience than managed DB; accepted for MVP.

3. Deploy strategy: simple restart with health check and rollback command
- Why: lower complexity and RAM usage than dual stack, suitable for 2 GB server.
- Safety baseline: deploy only after CI passes, run health check, keep previous git commit for fast rollback.

## Current implementation slice

- Auth API endpoints:
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
- JWT token generation and validation filter
- RBAC route guards and JSON auth error responses
- Ticket API endpoints:
  - `POST /api/v1/tickets`
  - `GET /api/v1/tickets`
  - `GET /api/v1/tickets/{id}`
  - `PATCH /api/v1/tickets/{id}/status`
  - `PATCH /api/v1/tickets/{id}/assign`
  - `POST /api/v1/tickets/{id}/comments`
  - `GET /api/v1/tickets/{id}/comments`
- Ticket access scoped by authenticated organization id
- Flyway baseline migration
- Flyway seed migration with demo tickets
- Flyway password migration for demo users
- Frontend panel for create and list
- Compose stack (postgres + backend + frontend)
- Nginx routing template
- CI/CD workflow skeleton

## Next implementation slices

1. Assignment and comments
- Assignment history tracking
- Comment edit/delete policy

2. SLA and metrics
- SLA policy entity
- Scheduled breach checker
- Dashboard aggregates

## Definition of done for Sprint 1

1. End-to-end create/list/status flow works locally via Docker
2. Basic integration tests in backend
3. Ubuntu deployment through SSH workflow
