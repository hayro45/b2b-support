#!/usr/bin/env bash
# Restore into a NEW database for verification. Never drops/cleans the live database.
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo "Usage: bash infra/scripts/restore.sh BACKUP.dump NEW_DATABASE_NAME" >&2
  exit 1
fi
backup_file=$1
restore_db=$2
[[ -f "$backup_file" && "$restore_db" =~ ^restore_[a-z0-9_]+$ && ${#restore_db} -le 63 ]] || {
  echo 'Supply an existing backup and a new database name starting with restore_.' >&2; exit 1;
}
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
env_file=${SUPPORT_ENV_FILE:-"$repo_dir/.env.production"}
test -f "$env_file"
compose=(docker compose --env-file "$env_file" -f "$repo_dir/infra/docker-compose.yml" -f "$repo_dir/infra/docker-compose.prod.yml")
"${compose[@]}" exec -T postgres pg_restore --list < "$backup_file" > /dev/null
# createdb fails if the target already exists; it cannot overwrite an existing DB.
"${compose[@]}" exec -T postgres sh -c 'exec createdb -U "$POSTGRES_USER" "$1"' sh "$restore_db"
"${compose[@]}" exec -T postgres sh -c 'exec pg_restore -U "$POSTGRES_USER" -d "$1" --no-owner --no-acl --exit-on-error --single-transaction' sh "$restore_db" < "$backup_file"
printf 'Restore verified into %s. Live database was not switched.\n' "$restore_db"
