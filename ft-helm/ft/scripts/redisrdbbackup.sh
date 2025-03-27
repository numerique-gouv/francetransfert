#!/bin/sh

mkdir -p /backup/backup-redis

touch /backup/backup-redis/ftr-data.rdb

echo $FT_REDIS_SERVICE_HOST

redis-cli -h "$FT_REDIS_SERVICE_HOST" -p "$FT_REDIS_SERVICE_PORT_TCP_REDIS" -a "$METALOAD_PASSWORD" --rdb "/backup/backup-redis/ftr-data.rdb"
# check if backup folder is empty
if [ -z "$(ls /backup/backup-redis)" ]; then
  echo "backup folder is empty"
  exit 1
fi
echo "finish copying dump file"
