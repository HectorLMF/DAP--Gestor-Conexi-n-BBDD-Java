#!/usr/bin/env bash
# test-connectivity.sh - verify web server and DB factories endpoints
set -euo pipefail
HOST=${1:-localhost}
PORT=${2:-8000}
BASEURL="http://$HOST:$PORT"

echo "Checking GET /"
if ! curl -sSf "$BASEURL/" >/dev/null; then
  echo "GET / failed" >&2
  exit 2
fi

echo "Checking GET /index.html"
curl -sSf "$BASEURL/index.html" >/dev/null || { echo "GET index.html failed" >&2; exit 2; }

# helper to POST query
post_query(){
  local db="$1"
  local payload='{"db":"'$db'","sql":"SELECT 1"}'
  echo "POST /query db=$db"
  http_code=$(curl -s -o /tmp/query_resp.json -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$payload" "$BASEURL/query")
  echo "HTTP $http_code"
  if [[ "$http_code" != "200" ]]; then
    echo "Response non-OK for db=$db. Body:" >&2
    cat /tmp/query_resp.json >&2
    return 2
  fi
  echo "Response body for $db:"; cat /tmp/query_resp.json
}

post_query "postgres"
post_query "mysql"

echo "All connectivity tests passed"
