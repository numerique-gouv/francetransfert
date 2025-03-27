#!/bin/sh

mkdir -p /backup/backup-redis
mkdir -p /data

redis-cli -h "$FT_REDIS_SERVICE_HOST" -p "$FT_REDIS_SERVICE_PORT_TCP_REDIS" -a "$METALOAD_PASSWORD" --rdb "/data/ftr-data.rdb"
redis-cli -h "$FT_REDIS_SERVICE_HOST" -p "$FT_REDIS_SERVICE_PORT_TCP_REDIS" -a "$METALOAD_PASSWORD" --rdb "ftr-data.rdb"
# check if backup folder is empty
if [ -z "$(ls /backup/backup-redis)" ]; then
  echo "backup folder is empty"
  exit 1
fi
echo "finish copying dump file"
