#!/usr/bin/env bash
# start.sh - Cross-platform (Linux/macOS) helper to start Postgres, MySQL and the Java app
# Usage: ./scripts/start.sh up|down|run|help [options]

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG="/tmp/dap_server.log"

ACTION=${1:-help}
shift || true

# defaults
PG_CONTAINER="dap-postgres"
PG_PORT=5432
PG_USER="postgres"
PG_PASSWORD="postgres"
PG_DATABASE="postgres"

MY_CONTAINER="dap-mysql"
MY_PORT=3306
MY_USER="root"
MY_PASSWORD="root"
MY_DATABASE="mysql"

FOREGROUND=1
START_MYSQL=0

usage(){
  cat <<EOF
Usage: $0 <up|down|run|simulate|help> [options]
Options (env or args):
  --pg-port PORT       host port for Postgres (default ${PG_PORT})
  --my-port PORT       host port for MySQL (default ${MY_PORT})
  --start-mysql        also start MySQL container
  --foreground|--bg    run app in foreground or background (default fg)

Examples:
  $0 up --start-mysql    # start Postgres + MySQL and run app in foreground
  $0 up --start-mysql --bg  # run in background (logs in $LOG)
  $0 down               # stop/remove both containers
EOF
}

# parse args simple
while [[ $# -gt 0 ]]; do
  case "$1" in
    --pg-port) PG_PORT="$2"; shift 2;;
    --my-port) MY_PORT="$2"; shift 2;;
    --start-mysql) START_MYSQL=1; shift;;
    --bg|--background) FOREGROUND=0; shift;;
    --foreground) FOREGROUND=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown option: $1"; usage; exit 1;;
  esac
done

check_docker(){
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker CLI not found in PATH. Please install or enable Docker." >&2
    return 1
  fi
  # quick docker ping depending on platform (Docker Desktop exposes different sockets)
  if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon not reachable. Ensure Docker is running and you have permission to use it." >&2
    return 1
  fi
  return 0
}

start_postgres(){
  echo "Starting Postgres container $PG_CONTAINER -> host:$PG_PORT"
  if ! check_docker; then return 1; fi
  existing=$(docker ps -a --filter "name=$PG_CONTAINER" -q || true)
  if [[ -n "$existing" ]]; then
    echo "Removing existing container $PG_CONTAINER"
    docker rm -f "$PG_CONTAINER" >/dev/null || true
  fi
  docker run --name "$PG_CONTAINER" -e POSTGRES_PASSWORD="$PG_PASSWORD" -e POSTGRES_USER="$PG_USER" -e POSTGRES_DB="$PG_DATABASE" -e POSTGRES_HOST_AUTH_METHOD=md5 -e POSTGRES_INITDB_ARGS="--auth-host=md5" -p "$PG_PORT":5432 -d postgres:16 >/dev/null
  echo "Waiting for Postgres readiness..."
  tries=0
  until docker exec "$PG_CONTAINER" pg_isready -U "$PG_USER" -d "$PG_DATABASE" >/dev/null 2>&1; do
    tries=$((tries+1))
    if [[ $tries -gt 30 ]]; then
      echo "Timeout waiting for Postgres" >&2
      return 2
    fi
    sleep 2
  done
  echo "Postgres ready"
}

start_mysql(){
  echo "Starting MySQL container $MY_CONTAINER -> host:$MY_PORT"
  if ! check_docker; then return 1; fi
  existing=$(docker ps -a --filter "name=$MY_CONTAINER" -q || true)
  if [[ -n "$existing" ]]; then
    echo "Removing existing container $MY_CONTAINER"
    docker rm -f "$MY_CONTAINER" >/dev/null || true
  fi
  docker run --name "$MY_CONTAINER" -e MYSQL_ROOT_PASSWORD="$MY_PASSWORD" -e MYSQL_DATABASE="$MY_DATABASE" -p "$MY_PORT":3306 -d mysql:8.1 >/dev/null
  echo "Waiting for MySQL readiness..."
  tries=0
  until docker exec "$MY_CONTAINER" mysqladmin ping -uroot -p"$MY_PASSWORD" >/dev/null 2>&1; do
    tries=$((tries+1))
    if [[ $tries -gt 60 ]]; then
      echo "Timeout waiting for MySQL" >&2
      return 2
    fi
    sleep 2
  done
  echo "MySQL ready"
}

stop_all(){
  echo "Stopping/removing containers if exist"
  docker rm -f "$PG_CONTAINER" >/dev/null 2>&1 || true
  docker rm -f "$MY_CONTAINER" >/dev/null 2>&1 || true
}

run_app(){
  echo "Building application (maven)"
  (cd "$ROOT_DIR" && mvn -DskipTests package)
  if [[ $FOREGROUND -eq 1 ]]; then
    echo "Running app in foreground"
    (cd "$ROOT_DIR" && mvn -DskipTests -Dexec.mainClass=org.example.Main exec:java)
  else
    echo "Running app in background, logs -> $LOG"
    nohup sh -c "cd '$ROOT_DIR' && mvn -DskipTests -Dexec.mainClass=org.example.Main exec:java" > "$LOG" 2>&1 &
    echo "App started (background). Tail logs with: tail -f $LOG"
  fi
}

case "$ACTION" in
  help) usage; exit 0;;
  down)
    stop_all
    exit 0;;
  simulate)
    echo "Simulate mode: no docker, setting env vars for host localhost"
    export PGHOST=localhost
    export PGPORT=$PG_PORT
    export PGDATABASE=$PG_DATABASE
    export PGUSER=$PG_USER
    export PGPASSWORD=$PG_PASSWORD
    if [[ $START_MYSQL -eq 1 ]]; then
      export MYSQL_HOST=localhost
      export MYSQL_PORT=$MY_PORT
      export MYSQL_DATABASE=$MY_DATABASE
      export MYSQL_USER=$MY_USER
      export MYSQL_PASSWORD=$MY_PASSWORD
    fi
    run_app
    ;;
  up)
    start_postgres || { echo "Failed to start Postgres"; exit 1; }
    export PGHOST=localhost
    export PGPORT=$PG_PORT
    export PGDATABASE=$PG_DATABASE
    export PGUSER=$PG_USER
    export PGPASSWORD=$PG_PASSWORD
    if [[ $START_MYSQL -eq 1 ]]; then
      start_mysql || { echo "Failed to start MySQL"; exit 1; }
      export MYSQL_HOST=localhost
      export MYSQL_PORT=$MY_PORT
      export MYSQL_DATABASE=$MY_DATABASE
      export MYSQL_USER=$MY_USER
      export MYSQL_PASSWORD=$MY_PASSWORD
    fi
    run_app
    ;;
  run)
    export PGHOST=localhost
    export PGPORT=$PG_PORT
    export PGDATABASE=$PG_DATABASE
    export PGUSER=$PG_USER
    export PGPASSWORD=$PG_PASSWORD
    if [[ $START_MYSQL -eq 1 ]]; then
      export MYSQL_HOST=localhost
      export MYSQL_PORT=$MY_PORT
      export MYSQL_DATABASE=$MY_DATABASE
      export MYSQL_USER=$MY_USER
      export MYSQL_PASSWORD=$MY_PASSWORD
    fi
    run_app
    ;;
  *)
    echo "Unknown action: $ACTION"; usage; exit 1;;
esac
