#!/bin/sh

set -eu

LOCK_FIELD="expired-timestamp"
LOCK_KEY_PREFIX="enclosure:"
SEND_KEY_PREFIX="send:"

if [ -z "${LOCK_EMAIL:-}" ] && [ -z "${LOCK_ENCLOSURE_IDS:-}" ]; then
  echo "LOCK_EMAIL and LOCK_ENCLOSURE_IDS are empty. Nothing to lock."
  exit 1
fi

echo "getting one redis pod"
pod=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-server" | sed "s/^.\{4\}//" | head -n 1)
if [ -z "${pod}" ]; then
  echo "failed to find a running redis pod"
  exit 1
fi

expired_timestamp=$(date -u -d "30 days ago" +"%Y-%m-%dT%H:%M" 2>/dev/null || true)
if [ -z "${expired_timestamp}" ]; then
  expired_timestamp=$(date -u -v-30d +"%Y-%m-%dT%H:%M" 2>/dev/null || true)
fi
if [ -z "${expired_timestamp}" ]; then
  echo "failed to compute expired timestamp"
  exit 1
fi

echo "using redis pod: ${pod}"
echo "expired timestamp: ${expired_timestamp}"

locked_count=0

lock_enclosure() {
  enclosure_id="$1"
  if [ -z "${enclosure_id}" ]; then
    return
  fi

  echo "HSET ${LOCK_KEY_PREFIX}${enclosure_id} ${LOCK_FIELD} ${expired_timestamp}"
  kubectl exec "$pod" -- redis-cli -a "$METALOAD_PASSWORD" HSET "${LOCK_KEY_PREFIX}${enclosure_id}" "${LOCK_FIELD}" "${expired_timestamp}" >/dev/null
  locked_count=$((locked_count + 1))
}

if [ -n "${LOCK_ENCLOSURE_IDS:-}" ]; then
  OLD_IFS=$IFS
  IFS=','
  for raw_enclosure in $LOCK_ENCLOSURE_IDS; do
    enclosure=$(echo "$raw_enclosure" | tr -d '[:space:]')
    lock_enclosure "$enclosure"
  done
  IFS=$OLD_IFS
fi

if [ -n "${LOCK_EMAIL:-}" ]; then
  send_key="${SEND_KEY_PREFIX}${LOCK_EMAIL}"
  echo "reading enclosures from ${send_key}"
  if ! enclosures_from_mail=$(kubectl exec "$pod" -- redis-cli -a "$METALOAD_PASSWORD" SMEMBERS "$send_key"); then
    echo "failed to read enclosure ids from ${send_key}"
    exit 1
  fi

  for enclosure in $enclosures_from_mail; do
    cleaned_enclosure=$(echo "$enclosure" | tr -d '[:space:]')
    lock_enclosure "$cleaned_enclosure"
  done
fi

if [ "$locked_count" -eq 0 ]; then
  echo "no enclosure id found to lock"
  exit 1
fi

echo "finished locking ${locked_count} enclosure(s)"
