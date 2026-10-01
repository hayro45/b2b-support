# Security

Report suspected vulnerabilities privately to the repository owner, not in a
public issue containing credentials, tokens, personal data or exploit details.

This is a single-host portfolio MVP, not a security certification. Production
requires the `prod` profile, private secrets and separately provisioned users.
Development credentials are intentionally public and rejected in production.

Git history is scanned by Gitleaks in CI. Its only allowed secret is the exact
public development JWT fixture, which the production application rejects.
Never commit dotenv files, database dumps, private keys or personal documents.

See the deployment runbook for backup, restoration and migration-aware rollback.
The owner-provisioning script creates a private mode-0600 credential file;
keep that file outside the repository and never paste its contents into issues.
