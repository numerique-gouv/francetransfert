#!/bin/sh
# get one redis slave pod
echo "getting redis-server pods"
pods=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-server" | sed "s/^.\{4\}//" || true)
if [ -z "${pods}" ]; then
  echo "failed to find a running redis pod"
  exit 1
fi

pod=""
for candidate in $pods; do
  role=$(kubectl exec "$candidate" -- redis-cli -a "$METALOAD_PASSWORD" INFO replication 2>/dev/null | grep "^role:" | tr -d '\r' || true)
  if [ "$role" = "role:slave" ] || [ "$role" = "role:replica" ]; then
    pod="$candidate"
    break
  fi
done

if [ -z "${pod}" ]; then
  echo "failed to find a redis slave pod"
  exit 1
fi

echo "using redis slave pod: ${pod}"
mkdir -p /backup/backup-redis

echo "BGSAVE"
kubectl exec $pod -- redis-cli -a "$METALOAD_PASSWORD" BGSAVE

finished=0
while [ $finished -eq 0 ]; do
  sleep 2
  echo "WAIT FOR BGSAVE"
  kubectl exec $pod -- redis-cli -a "$METALOAD_PASSWORD" INFO PERSISTENCE | grep -q "rdb_bgsave_in_progress:1"
  finished=$?
done

finished=0
echo "BGREWRITEAOF"
kubectl exec $pod -- redis-cli -a "$METALOAD_PASSWORD" BGREWRITEAOF
finished=0
while [ $finished -eq 0 ]; do
  sleep 2
  echo "WAIT FOR BGREWRITEAOF"
  kubectl exec $pod -- redis-cli -a "$METALOAD_PASSWORD" INFO PERSISTENCE | grep -q "aof_rewrite_in_progress:1"
  finished=$?
done


# echo "delete old dump file"
# kubectl exec $pod -- rm -f /data/*.rdb

echo "copy dump file from $pod to /backup/backup-redis"
kubectl cp $pod:/data /backup/backup-redis
ls -lrt /backup/backup-redis
if [ $? -ne 0 ]; then
  echo "failed to copy dump file"
  exit 1
fi
# check if backup folder is empty
if [ -z "$(ls /backup/backup-redis)" ]; then
  echo "backup folder is empty"
  exit 1
fi
echo "finish copying dump file"
