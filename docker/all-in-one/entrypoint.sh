#!/usr/bin/env bash
set -euo pipefail

PG_VERSION="$(ls /etc/postgresql | sort -V | tail -n 1)"
PG_DATA_DIR="/var/lib/postgresql/${PG_VERSION}/main"
REDIS_PID=""
BACKEND_PID=""
NGINX_PID=""

log() {
  echo "[all-in-one] $1"
}

cleanup() {
  local code=$?
  trap - EXIT SIGINT SIGTERM
  log "Shutting down services..."
  if [[ -n "${NGINX_PID}" ]] && kill -0 "${NGINX_PID}" 2>/dev/null; then
    kill -TERM "${NGINX_PID}" || true
  fi
  if [[ -n "${BACKEND_PID}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
    kill -TERM "${BACKEND_PID}" || true
  fi
  if [[ -n "${REDIS_PID}" ]] && kill -0 "${REDIS_PID}" 2>/dev/null; then
    kill -TERM "${REDIS_PID}" || true
  fi
  pg_ctlcluster --skip-systemctl-redirect "${PG_VERSION}" main stop || true
  wait || true
  exit $code
}

trap cleanup EXIT SIGINT SIGTERM

mkdir -p /data /app/uploads /var/run/postgresql
chown -R postgres:postgres /data /var/lib/postgresql /var/run/postgresql

if [[ ! -d "${PG_DATA_DIR}/base" ]]; then
  log "Initializing PostgreSQL cluster..."
  pg_dropcluster --stop "${PG_VERSION}" main || true
  pg_createcluster "${PG_VERSION}" main
fi

log "Starting PostgreSQL..."
pg_ctlcluster --skip-systemctl-redirect "${PG_VERSION}" main start

until pg_isready -h 127.0.0.1 -p 5432 -U postgres >/dev/null 2>&1; do
  sleep 1
done

log "Configuring PostgreSQL database and user..."
su postgres -c "psql -v ON_ERROR_STOP=1 --dbname postgres <<'SQL'
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'psychology_user') THEN
    CREATE ROLE psychology_user LOGIN PASSWORD 'psychology_pass';
  ELSE
    ALTER ROLE psychology_user WITH LOGIN PASSWORD 'psychology_pass';
  END IF;
END
\$\$;
SQL"

if ! su postgres -c "psql -tAc \"SELECT 1 FROM pg_database WHERE datname='psychology_db'\"" | grep -q 1; then
  su postgres -c "createdb -O psychology_user psychology_db"
fi

log "Starting Redis..."
redis-server --bind 127.0.0.1 --port 6379 --appendonly yes --dir /data &
REDIS_PID=$!

log "Starting backend..."
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://127.0.0.1:5432/psychology_db}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-psychology_user}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-psychology_pass}"
export SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-127.0.0.1}"
export SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}"
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-update}"
export APP_SEED_ENABLED="${APP_SEED_ENABLED:-true}"
export APP_PSYCHOLOGISTS_REQUIRE_VERIFICATION="${APP_PSYCHOLOGISTS_REQUIRE_VERIFICATION:-false}"
export APP_ORGANIZATION_NAME="${APP_ORGANIZATION_NAME:-ООО «Телеком без границ»}"

sh -c "java ${JAVA_OPTS:-} -jar /app/app.jar" &
BACKEND_PID=$!

log "Starting Nginx..."
nginx -g "daemon off;" &
NGINX_PID=$!

wait -n "$REDIS_PID" "$BACKEND_PID" "$NGINX_PID"
