#!/bin/sh

set -eu

QUEUE_NAME="sequestre-queue"

if [ -z "${ENCLOSURE_LIST:-}" ]; then
  echo "ENCLOSURE_LIST is empty. Nothing to enqueue."
  exit 1
fi

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

OLD_IFS=$IFS
IFS=','
for raw_enclosure in $ENCLOSURE_LIST; do
  enclosure=$(echo "$raw_enclosure" | tr -d '[:space:]')
  if [ -z "$enclosure" ]; then
    continue
  fi

  echo "RPUSH ${QUEUE_NAME} ${enclosure}"
  kubectl exec "$pod" -- redis-cli -a "$METALOAD_PASSWORD" RPUSH "$QUEUE_NAME" "$enclosure"
done
IFS=$OLD_IFS

echo "finished enqueueing enclosure list"
