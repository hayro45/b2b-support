# Hardening pass — 1 October 2026

## Team and scope

Three parallel agents owned separate areas: backend/auth/data/tests, frontend/product/tooling/tests, and infrastructure/deployment/recovery. The coordinating agent reviewed the interfaces, updated documentation and tested the integrated application in an isolated local Docker project.

The target remained a single-host MVP for a small team. No SLA engine, microservices, SSO or attachment pipeline was added. Existing unrelated `Career_Command_Centre/`, `output/` and `tmp/` content was preserved.

## Verification results

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Backend suite | 24 passed; no failures/errors/skips | 20 PostgreSQL integration tests through Testcontainers + 4 unit tests |
| Frontend suite | 9 passed | Roles, API errors, create/status/assignment, filters/paging and stale-response regression |
| Frontend lint | Pass, zero warnings | Real ESLint replaces the placeholder script |
| Production frontend build | Pass | TypeScript and Vite; JS approximately 236 KB / 73 KB gzip |
| Full npm audit | 0 vulnerabilities | Includes development dependencies; does not claim a full-system security audit |
| Docker images | Both built successfully | Node 24; frontend uid101, backend uid999 |
| Compose | Development/production configs valid | Production missing-secret configuration rejected |
| Nginx | Container configuration valid | Local `/api` proxy and CSP/security headers verified |
| Shell scripts | Syntax and input guards passed | Invalid deploy SHA/rollback acknowledgement/restore target rejected |
| Browser workflow | Passed on real local API/database | Created ticket → changed status → assigned staff → public reply + internal note |
| Customer visibility | Passed | Internal note, staff actions and assignment history absent |
| Responsive layout | Passed at 320 px | No horizontal document overflow; screenshot retained |
| Browser console | No captured errors/warnings | During the exercised workflows |
| Production-profile startup | Passed with explicit QA secrets | Healthy; seeded accounts inactive; demo login 401; separately provisioned BCrypt staff login succeeded |
| Migration compatibility | V1–V5 preserved | V6 applied and schema validated with Hibernate |
| Backup / restore | Actual archive restored into a new QA DB | 3 tickets, 2 users, 7 tables, 6 successful migrations; duplicate restore refused without overwriting |
| Public deployment | Not changed | No push, workflow dispatch, remote account/secret change or live deployment |

The final API check also corrected missing-resource and unsupported-method errors to 404/405 instead of generic 500. Production OpenAPI is disabled. Host TLS/HSTS configuration is a template requiring actual certificates; the local HTTP preview is not evidence that the public host has adopted those headers.

## Review artifacts

- [Agent workspace](../screenshots/agent-workspace.jpg)
- [Customer workspace](../screenshots/customer-workspace.jpg)
- [320 px customer view](../screenshots/customer-mobile.jpg)
- [Deployment and recovery runbook](../runbook/ubuntu-single-server-deploy.md)
- [Updated product backlog](../backlog/sprint-backlog.md)

The review preview runs at `http://localhost:13000` in project `support-qa-20261001`; its API port is `18080`. It has a separate Docker volume and fictional test records. A production-profile rehearsal used a separate database, and the restore rehearsal created another new database. No existing user database was replaced or dropped. Portable Java/Maven verification tools were downloaded from official sources with checksums into the OS temporary directory, without changing system PATH.

## Remaining operator work and deliberate limits

Before live deployment, provision non-demo accounts, supply production secrets, verify the existing database credential, configure SSH/environment protection, and take an off-host backup. The production profile will disable current demo accounts. A rollback restores application images only; it cannot reverse Flyway or data changes.

Comment/history pagination, user/password provisioning UI, UTC conversion of historical timestamps, SLA/dashboard and client version preconditions remain in the backlog. In-memory login throttling is designed for one API instance. These are recorded limitations, not completed features.
