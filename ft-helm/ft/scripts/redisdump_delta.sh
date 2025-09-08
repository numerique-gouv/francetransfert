#!/bin/sh
# get one redis pod
pod=$(kubectl get pods -o=name --field-selector status.phase=Running | grep "redis-node" | sed "s/^.\{4\}//" | head -n 1)
mkdir -p /backup/backup-redis

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
