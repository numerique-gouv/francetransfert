#!/bin/sh

set -eu

TTL_SECONDS=${TTL_SECONDS:-34160000}
PREFIXES="enclosure-date: enclosure-dates: enclosure: recipient: root-dir: root-file: sender:"

# Keys with quotes/pipes/spaces/newlines stay inside Redis (never parsed by shell)
LUA_SCRIPT=$(cat <<'EOF'
local cursor = "0"
local updated = 0
local skipped = 0
local ttl = tonumber(ARGV[1])
local pattern = ARGV[2]
repeat
  local result = redis.call("SCAN", cursor, "MATCH", pattern, "COUNT", 100)
  cursor = result[1]
  for _, key in ipairs(result[2]) do
    if redis.call("TTL", key) == -1 then
      redis.call("EXPIRE", key, ttl)
      updated = updated + 1
    else
      skipped = skipped + 1
    end
  end
until cursor == "0"
return {updated, skipped}
EOF
)

echo "getting redis-server pods"
pods=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-server" | sed "s/^.\{4\}//" || true)
if [ -z "${pods}" ]; then
  echo "failed to find a running redis pod"
  exit 1
fi

pod=""
for candidate in $pods; do
  role=$(kubectl exec "$candidate" -- redis-cli -a "$METALOAD_PASSWORD" INFO replication 2>/dev/null | grep "^role:" | tr -d '\r' || true)
  if [ "$role" = "role:master" ]; then
    pod="$candidate"
    break
  fi
done

if [ -z "${pod}" ]; then
  echo "failed to find a redis master pod"
  exit 1
fi

echo "using redis master pod: ${pod}"
echo "setting TTL of ${TTL_SECONDS}s (12 months) on keys without expiry"

updated_total=0
skipped_total=0

for prefix in $PREFIXES; do
  pattern="${prefix}*"
  echo "scanning prefix: ${pattern}"
  result=$(kubectl exec "$pod" -- redis-cli -a "$METALOAD_PASSWORD" --raw EVAL "$LUA_SCRIPT" 0 "$TTL_SECONDS" "$pattern")
  updated=$(echo "$result" | sed -n '1p' | tr -d '\r')
  skipped=$(echo "$result" | sed -n '2p' | tr -d '\r')
  echo "prefix ${prefix}: updated=${updated} skipped=${skipped}"
  updated_total=$((updated_total + updated))
  skipped_total=$((skipped_total + skipped))
done

echo "finished: updated=${updated_total} skipped=${skipped_total}"
