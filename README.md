# B2B Support Ticket System

Production-oriented full-stack MVP for career proof:
- Backend: Java 21, Spring Boot, PostgreSQL, Flyway
- Frontend: React + TypeScript + Vite
- Infra: Docker Compose, Nginx reverse proxy, GitHub Actions CI/CD

## Monorepo Structure

- `backend/`: Spring Boot API
- `frontend/`: React web app
- `infra/`: Docker Compose and Nginx configs
- `docs/`: Architecture and runbooks

## Quick Start (Local)

1. Copy env file:
   - PowerShell: `Copy-Item .env.example .env`
2. Start stack:
   - `docker compose -f infra/docker-compose.yml up -d --build`
3. Open:
   - Frontend: `http://localhost:3000`
   - Backend: `http://localhost:8080`
   - API docs: `http://localhost:8080/swagger-ui/index.html`

## Backend Verification

Requirements: Java 21, Maven, and Docker Desktop with the Linux engine running.

```bash
cd backend
mvn test
```

The integration test uses Testcontainers and validates Flyway migrations, demo seed data, JWT login, protected ticket access, and unauthorized access behavior.

## Demo Credentials

- Agent: `agent@demo.local` / `demo12345`
- Customer: `customer@demo.local` / `demo12345`

## Auth Flow

1. Login via `POST /api/v1/auth/login`
2. Use `Authorization: Bearer <token>` for ticket endpoints
3. Current RBAC:
   - `GET /api/v1/tickets/**`: CUSTOMER, AGENT, ADMIN
   - `POST /api/v1/tickets/**`: CUSTOMER, AGENT, ADMIN
   - `PATCH /api/v1/tickets/**`: AGENT, ADMIN

## MVP Slice Included

- JWT login endpoint
- Role-based authorization rules
- Organization-scoped ticket access
- Create ticket
- List tickets with paging and filters
- Get ticket by id
- Change ticket status with transition rules
- Assign ticket to an agent
- Add and list ticket comments (with internal note support)
- Auto-loaded demo seed data

## Next Milestones

- Assignment and comments
- SLA policy and breach detection
- CI/CD deploy to Ubuntu server
