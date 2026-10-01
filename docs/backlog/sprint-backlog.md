# MVP backlog

This tracks repository implementation. Deployment status must be verified separately; local edits are not evidence that the public server has been upgraded.

## Implemented in the hardening pass

- [x] Transactional ticket writes with audit and assignment-history consistency.
- [x] Database-sequence ticket numbers and optimistic locking.
- [x] Combined status/priority/title filters and bounded paging/sort validation.
- [x] Active organization staff assignment and staff-only directory/history.
- [x] Customer internal notes filtered before the database result limit.
- [x] Legacy plaintext password hashes upgraded through a new migration; historical migrations retained.
- [x] Production rejects development secrets and active plaintext passwords; seeded demo accounts disabled.
- [x] Disabled accounts and current roles checked after JWT issuance.
- [x] Bounded single-instance login throttling.
- [x] Supported frontend toolchain, real linting and component/API tests.
- [x] Role-aware login, sign-out, session expiry and accessible errors.
- [x] Queue search/filters/paging and a ticket detail panel.
- [x] Staff status/assignment actions, public/internal comments and assignment timeline.
- [x] Local container API proxy and responsive layout.
- [x] Non-root containers, deterministic frontend install and Docker build context exclusions.
- [x] Explicit production environment, TLS/security header template and manual CI-gated deployment.
- [x] Health readiness wait, saved application images and explicit schema-compatible rollback.
- [x] Private database backup and non-overwriting restore rehearsal scripts.
- [x] README/architecture/runbook aligned with the implemented product.

## Production rollout (operator work)

- [ ] Provision non-demo staff/customer accounts with encoded passwords.
- [ ] Supply strong production secrets and verify existing database credentials.
- [ ] Configure deployment SSH fingerprint and GitHub environment protection.
- [ ] Take and restore-test an off-host backup before changing live data.
- [ ] Deploy the tested commit and verify customer/staff workflows on HTTPS.
- [ ] Confirm security headers and certificate auto-renewal on the live proxy.

## Next useful increments

1. User provisioning/password-change workflow; SSO only if requested by a real team.
2. Full comment/history paging, staff audit view and agreed comment edit/delete policy.
3. SLA policy and breach detection, followed by dashboard aggregates.
4. UTC timestamp migration with a stated original timezone.
5. Client version preconditions for edits made from stale screens.
6. Notifications and attachments when there is a product requirement.
7. Multi-node/shared throttling or higher availability when operational requirements justify the cost.

SLA, file upload, SSO, email notifications and a metrics dashboard are intentionally outside the current pass. Do not mark them complete based on directory structure or planned endpoints.
