# Sprint Backlog

## Sprint 1 (in progress)

Scope lock (confirmed)
- [x] Target launch audience: 5-6 users
- [x] File upload excluded from MVP
- [x] Demo seed data auto-loaded via Flyway
- [x] Production routing model: single subdomain with `/api`
- [x] DB model: PostgreSQL container on same host
- [x] Deploy strategy: simple restart + health check

1. Project skeleton and environments
- [x] Monorepo layout
- [x] Docker compose runtime
- [x] Local env template

2. Backend ticket MVP
- [x] Migration baseline for org, users, tickets
- [x] Ticket create/list/get/status endpoints
- [x] Global error responses
- [x] JWT login endpoint and token filter
- [x] RBAC route guards
- [ ] Integration tests for ticket flow

3. Frontend MVP
- [x] Auth login panel with JWT usage
- [x] Ticket create form
- [x] Ticket list and status filter
- [ ] Ticket detail page

4. Delivery and ops
- [x] CI/CD workflow draft
- [x] Ubuntu deployment runbook
- [ ] Production Nginx activation on server

## Sprint 2 (planned)

1. Assignment history
2. Comment edit/delete policy
3. Audit logging

Sprint 2 progress carried early
- [x] Ticket assign endpoint (`PATCH /api/v1/tickets/{id}/assign`)
- [x] Ticket comment endpoints (`POST/GET /api/v1/tickets/{id}/comments`)

## Sprint 3 (planned)

1. SLA policy CRUD
2. SLA breach scheduler
3. Dashboard endpoints
4. Hardening and observability
