# Ubuntu single-server deployment and recovery

This runbook is for an Ubuntu host running a 5–6-user support desk. Local source edits do not change the public deployment. A deploy should follow a green CI run and a verified database backup.

## Prerequisites

Install Docker Engine and the Compose plugin from the [official Ubuntu instructions](https://docs.docker.com/engine/install/ubuntu/), Git, curl, OpenSSL and host Nginx. Keep an existing personal website in its own Nginx server block. Allow SSH, HTTP and HTTPS in the host firewall. Verify DNS points to the correct server.

```bash
docker version
docker compose version
sudo nginx -t
```

Clone the repository into `/opt/b2b-support` as the deployment user. SSH access should use a dedicated deployment key; verify the server's SSH host fingerprint independently.

## Production configuration

```bash
cd /opt/b2b-support
cp infra/.env.production.example .env.production
chmod 600 .env.production
openssl rand -base64 48
```

Edit `.env.production` and supply:
- `POSTGRES_PASSWORD`: a unique strong database password.
- `APP_JWT_SECRET`: the generated Base64 value; never use the development key.
- `APP_CORS_ALLOWED_ORIGINS`: the exact public HTTPS origin.
- `POSTGRES_DB` and `POSTGRES_USER`: preserve existing values when upgrading.

Do not commit or paste secrets into issue/CI logs. The production Compose override rejects missing secrets. The backend production profile also rejects development secrets/plaintext active passwords and disables the historical `@demo.local` seed accounts.

Changing `POSTGRES_PASSWORD` in an environment file does **not** rotate the password of an existing PostgreSQL volume. When upgrading an existing instance, use its current credential or perform a coordinated database password rotation. Otherwise the backend loses connectivity.

The historical demo rows remain in the database because already-applied Flyway migrations must not be rewritten. V6 upgrades legacy password hashes, but production access requires a separately provisioned organization/staff account. Provision using the existing schema and a BCrypt password encoder; this MVP has no public registration or administrator provisioning UI. Never activate demo accounts for real customer data.

## Start or upgrade

```bash
docker compose --env-file .env.production \
  -f infra/docker-compose.yml -f infra/docker-compose.prod.yml \
  config --quiet
docker compose --env-file .env.production \
  -f infra/docker-compose.yml -f infra/docker-compose.prod.yml \
  up -d --build --wait --wait-timeout 180
```

API and frontend ports bind to `127.0.0.1`. PostgreSQL is not exposed. Check:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:3000/
```

Health is accessed directly on the API port. `/api/actuator/health` is not the actuator endpoint.

## TLS and Nginx

The sample `infra/nginx/support.hayrettindal.com.conf` includes HTTPS certificate paths, an HTTP redirect and security headers. Obtain the certificate **before** installing this final configuration. For a new host, bootstrap an HTTP-only server for Certbot, or use DNS validation; do not enable a TLS server referencing missing certificate files.

After a valid certificate exists:

```bash
sudo cp infra/nginx/support.hayrettindal.com.conf /etc/nginx/sites-available/support.hayrettindal.com.conf
# Link once; if the link already exists, inspect it rather than replacing blindly.
sudo ln -s /etc/nginx/sites-available/support.hayrettindal.com.conf /etc/nginx/sites-enabled/support.hayrettindal.com.conf
sudo nginx -t
sudo systemctl reload nginx
```

The public frontend is `https://support.hayrettindal.com`, and API paths start with `/api/v1`. Verify login, customer/staff roles, comments and HTTP-to-HTTPS redirect after deployment. Verify certificate renewal is scheduled. HSTS is applied only on the HTTPS server.

## GitHub deployment

Pushes and pull requests run CI. They do not automatically deploy. Configure a GitHub `production` environment and the following secrets:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_FINGERPRINT` (verified SSH server fingerprint)

Add required reviewers to the environment if available for the repository plan. The workflow references the environment but cannot create its protection rules.

Use **Actions → CI-CD → Run workflow**, choose `main`, and enable `deploy`. The workflow tests the selected commit, fetches it on the server and deploys that exact SHA. The server checkout must be free of tracked edits.

The deploy script builds images before replacing running containers, takes a database backup when PostgreSQL is already running, saves previous images and waits up to 180 seconds for container health. A failed deployment remains failed; image rollback requires an explicit compatibility check.

## Backups and restore rehearsal

```bash
bash infra/scripts/backup.sh
# Optional destination:
bash infra/scripts/backup.sh /var/backups/b2b-support
```

Backups use PostgreSQL custom format, private file permissions, unique filenames and a `pg_restore --list` check. The script does not delete old backups. Copy backups to storage outside this host and set a retention schedule appropriate to the data. A local backup alone does not survive host failure.

Rehearse restoration into a new database:

```bash
bash infra/scripts/restore.sh /var/backups/b2b-support/support-TIMESTAMP.dump restore_rehearsal_20261001
```

The target must start with `restore_` and must not exist. The script never drops or overwrites the live database. After restoring, inspect table counts, migration history and representative records. Switching the application to a restored database is a separate operator decision and can lose changes made after the backup.

## Rollback

```bash
bash infra/scripts/rollback.sh --schema-compatible
```

This restarts the previous **application images** only. It does not reverse Flyway, undo demo-user disabling, or revert database records. Only use it after confirming the previous application supports the current schema. If migration recovery is required, restore into a new database, validate it and plan the cutover.

## Routine operations

```bash
docker compose --env-file .env.production -f infra/docker-compose.yml -f infra/docker-compose.prod.yml ps
docker compose --env-file .env.production -f infra/docker-compose.yml -f infra/docker-compose.prod.yml logs --tail=100 backend
```

Watch disk usage, backup success, certificate renewal and application health. Update supported runtime/container patches regularly. Do not remove volumes or prune saved rollback images as a routine deployment step.

## Existing production deployment (1 October 2026)

The existing host keeps `/opt/b2b-support` intact as the legacy checkout, including
its untracked operator documents. Releases live in `/opt/b2b-support-releases/`;
`/opt/b2b-support-current` points at the active release. Production Compose uses
project name `infra`, retaining the existing `infra_postgres_data` volume.
The CD workflow uses the current-release path and explicitly retains that project
name. Do not run Compose with a new project name against existing data.

Production owner credentials are private, not in this repo. The one-time
`infra/scripts/prepare-production.py` creates mode-0600 env/access files and
refuses to overwrite them or replace an existing owner's password. JWT rotation
invalidates existing tokens. Changing the env database password alone does NOT
rotate an existing PostgreSQL user's password.

The PostgreSQL image retains upstream PostgreSQL 16 and its entrypoint/data
layout. It installs Alpine's native `su-exec` and uses it for the entrypoint's
privilege drop instead of upstream's Go-based `gosu`. Test both fresh database
initialization and reopening an existing volume when changing this image.
