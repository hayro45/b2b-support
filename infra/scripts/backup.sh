#!/usr/bin/env bash
# Read-only DB backup, private file; never removes older backups.
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
env_file=${SUPPORT_ENV_FILE:-"$repo_dir/.env.production"}
test -f "$env_file" || { echo "Missing production environment file: $env_file" >&2; exit 1; }
backup_dir=${1:-"$repo_dir/infra/backups"}
mkdir -p "$backup_dir"
backup_file="$backup_dir/support-$(date -u +%Y%m%dT%H%M%S)-$$.dump"
compose=(docker compose --env-file "$env_file" -f "$repo_dir/infra/docker-compose.yml" -f "$repo_dir/infra/docker-compose.prod.yml")
set -o noclobber
"${compose[@]}" exec -T --interactive=false postgres sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$backup_file"
test -s "$backup_file"
"${compose[@]}" exec -T postgres pg_restore --list < "$backup_file" > /dev/null
printf 'Backup verified: %s\n' "$backup_file"
