#!/usr/bin/env bash
set -euo pipefail
[[ ${1:-} =~ ^[a-f0-9]{40}$ ]] || { echo 'Supply the tested full Git commit SHA.' >&2; exit 1; }
export SUPPORT_IMAGE_TAG=$1
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
[[ $(git -C "$repo_dir" rev-parse HEAD) == "$SUPPORT_IMAGE_TAG" ]] || {
  echo 'Requested SHA must match the checked-out commit.' >&2; exit 1;
}
[[ -z $(git -C "$repo_dir" status --porcelain) ]] || {
  echo 'Deployment checkout must be clean (including non-ignored untracked files).' >&2; exit 1;
}
env_file=${SUPPORT_ENV_FILE:-"$repo_dir/.env.production"}
test -f "$env_file"
compose=(docker compose --env-file "$env_file" -f "$repo_dir/infra/docker-compose.yml" -f "$repo_dir/infra/docker-compose.prod.yml")
"${compose[@]}" config --quiet
# Build first: a build failure leaves running containers untouched.
"${compose[@]}" build --pull postgres backend frontend
if [[ -n $("${compose[@]}" ps -q postgres) ]]; then
  bash "$repo_dir/infra/scripts/backup.sh"
fi
for service in backend frontend; do
  container_id=$("${compose[@]}" ps -q "$service")
  if [[ -n "$container_id" ]]; then
    old_image=$(docker inspect --format '{{.Image}}' "$container_id")
    docker image tag "$old_image" "support-$service:previous"
  fi
done
if ! "${compose[@]}" up -d --no-build --wait --wait-timeout 180; then
  "${compose[@]}" ps
  echo 'Deployment failed health checks. Previous images remain tagged :previous.' >&2
  echo 'Assess migration compatibility before running rollback.sh --schema-compatible.' >&2
  exit 1
fi
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health > /dev/null
curl --fail --silent --show-error http://127.0.0.1:3000/ > /dev/null
printf 'Deployment healthy: %s\n' "$SUPPORT_IMAGE_TAG"
