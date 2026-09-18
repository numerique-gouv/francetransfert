#!/bin/sh

set -eu

TTL_SECONDS=${TTL_SECONDS:-34160000}
PREFIXES="enclosure-date: enclosure-dates: enclosure: recipient: root-dir: root-file: sender:"

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

# Run SCAN/TTL/EXPIRE inside one kubectl exec to avoid one exec per key
kubectl exec "$pod" -- env \
  METALOAD_PASSWORD="$METALOAD_PASSWORD" \
  TTL_SECONDS="$TTL_SECONDS" \
  PREFIXES="$PREFIXES" \
  sh -c '
set -eu

updated=0
skipped=0

for prefix in $PREFIXES; do
  echo "scanning prefix: ${prefix}*"
  cursor=0
  while true; do
    # SCAN --raw: cursor on first line, then matching keys
    result=$(redis-cli -a "$METALOAD_PASSWORD" --raw SCAN "$cursor" MATCH "${prefix}*" COUNT 100)
    cursor=$(echo "$result" | head -n 1 | tr -d "\r")
    keys=$(echo "$result" | tail -n +2)

    for key in $keys; do
      key=$(echo "$key" | tr -d "\r")
      if [ -z "$key" ]; then
        continue
      fi
      current_ttl=$(redis-cli -a "$METALOAD_PASSWORD" TTL "$key" | tr -d "\r")
      if [ "$current_ttl" = "-1" ]; then
        redis-cli -a "$METALOAD_PASSWORD" EXPIRE "$key" "$TTL_SECONDS" >/dev/null
        updated=$((updated + 1))
        echo "EXPIRE $key $TTL_SECONDS"
      else
        skipped=$((skipped + 1))
      fi
    done

    if [ "$cursor" = "0" ]; then
      break
    fi
  done
done

echo "finished: updated=${updated} skipped=${skipped}"
'
