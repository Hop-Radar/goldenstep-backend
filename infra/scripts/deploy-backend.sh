#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

DEPLOY_DIR="/opt/goldenstep"
COMPOSE_FILE="$DEPLOY_DIR/infra/compose.prod.yaml"
ENV_FILE="$DEPLOY_DIR/.env.prod"
NEW_IMAGE="${1:?Usage: deploy-backend.sh <ECR image URI>}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"

# Expect a private ECR image tagged with a full Git commit SHA.
if [[ ! "$NEW_IMAGE" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/[a-z0-9_-]+:([a-f0-9]{40}|[a-f0-9]{64})$ ]]; then
  echo "Invalid ECR image URI or Git commit SHA tag." >&2
  exit 1
fi

for tool in docker aws jq flock awk; do
  command -v "$tool" >/dev/null || {
    echo "Required command is missing: $tool" >&2
    exit 1
  }
done

[[ -f "$COMPOSE_FILE" && -f "$ENV_FILE" ]] || {
  echo "Compose file or production environment file is missing." >&2
  exit 1
}

# All service deployment scripts must use this same lock.
exec 9>"$DEPLOY_DIR/.deployment.lock"
flock -n 9 || {
  echo "Another deployment is running." >&2
  exit 1
}

# Use the production file as the source of the backend image setting.
unset BACKEND_IMAGE

compose() {
  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    "$@"
}

compose config --quiet

# Require exactly one explicit BACKEND_IMAGE entry.
ENTRY_COUNT="$(awk '/^BACKEND_IMAGE=/ { count++ } END { print count+0 }' "$ENV_FILE")"
[[ "$ENTRY_COUNT" == "1" ]] || {
  echo "Expected exactly one BACKEND_IMAGE entry." >&2
  exit 1
}

OLD_IMAGE="$(awk '/^BACKEND_IMAGE=/ {
  sub(/^BACKEND_IMAGE=/, "")
  print
}' "$ENV_FILE")"

[[ "${OLD_IMAGE%:*}" == "${NEW_IMAGE%:*}" ]] || {
  echo "The new image must use the current backend ECR repository." >&2
  exit 1
}

BACKEND_ID="$(compose ps -q backend)"
FRONTEND_ID="$(compose ps -q frontend)"

[[ -n "$BACKEND_ID" && -n "$FRONTEND_ID" ]] || {
  echo "Backend and frontend must already be running." >&2
  exit 1
}

RUNNING_IMAGE="$(docker inspect --format '{{.Config.Image}}' "$BACKEND_ID")"
[[ "$RUNNING_IMAGE" == "$OLD_IMAGE" ]] || {
  echo "Running backend image differs from the production environment file." >&2
  exit 1
}

# Verify the frontend tools needed for deployment checks.
compose exec -T frontend sh -c \
  'command -v wget >/dev/null && command -v nginx >/dev/null'

check_backend() {
  local response

  response="$(compose exec -T frontend \
    wget -q -T 5 -O - http://backend:8080/api/health)" || return 1

  jq -e \
    '.status == "UP" and .spring == "UP" and .fastApi == "UP"' \
    >/dev/null <<< "$response"
}

wait_for_backend() {
  local attempt

  for attempt in {1..36}; do
    if check_backend; then
      return 0
    fi
    sleep 5
  done

  return 1
}

reload_nginx() {
  compose exec -T frontend nginx -t &&
    compose exec -T frontend nginx -s reload
}

# Confirm the current stack is healthy before changing it.
check_backend || {
  echo "Current backend or FastAPI is unavailable. Deployment cancelled." >&2
  exit 1
}

REGISTRY="${NEW_IMAGE%%/*}"

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$REGISTRY"

# Download before stopping the existing backend.
docker pull "$NEW_IMAGE"

BACKUP_FILE="$(mktemp "$DEPLOY_DIR/.env.prod.backup.XXXXXX")"
NEXT_FILE="$(mktemp "$DEPLOY_DIR/.env.prod.next.XXXXXX")"
cp "$ENV_FILE" "$BACKUP_FILE"

CHANGED=0

cleanup() {
  local exit_code=$?
  trap - EXIT
  set +e

  if [[ "$exit_code" -ne 0 && "$CHANGED" -eq 1 ]]; then
    echo "Deployment failed. Restoring the previous backend image." >&2

    cp "$BACKUP_FILE" "$ENV_FILE"

    if compose up -d --no-deps --pull never backend &&
       wait_for_backend &&
       reload_nginx; then
      echo "Previous backend image restored." >&2
    else
      echo "Rollback failed. Manual inspection is required." >&2
    fi
  fi

  rm -f "$BACKUP_FILE" "$NEXT_FILE"
  exit "$exit_code"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

awk -v image="$NEW_IMAGE" '
  /^BACKEND_IMAGE=/ {
    print "BACKEND_IMAGE=" image
    next
  }
  { print }
' "$ENV_FILE" > "$NEXT_FILE"

CHANGED=1
mv "$NEXT_FILE" "$ENV_FILE"

compose config --quiet
compose up -d --no-deps --pull never backend

wait_for_backend || {
  echo "Backend readiness check failed." >&2
  exit 1
}

# Refresh Nginx upstream resolution after container replacement.
reload_nginx

echo "Backend deployment completed."