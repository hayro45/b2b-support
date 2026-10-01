#!/usr/bin/env bash
# Rollback application images only; this does NOT revert Flyway/schema/data changes.
set -euo pipefail
[[ ${1:-} == --schema-compatible ]] || {
  echo 'After verifying the old application supports the current DB schema, pass --schema-compatible.' >&2; exit 1;
}
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
env_file=${SUPPORT_ENV_FILE:-"$repo_dir/.env.production"}
test -f "$env_file"
docker image inspect support-backend:previous support-frontend:previous > /dev/null
export SUPPORT_IMAGE_TAG=previous
docker compose --env-file "$env_file" -f "$repo_dir/infra/docker-compose.yml" -f "$repo_dir/infra/docker-compose.prod.yml" up -d --no-build --wait --wait-timeout 180 backend frontend
echo 'Previous application images are healthy. Database schema/data were not rolled back.'
