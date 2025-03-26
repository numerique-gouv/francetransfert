#!/bin/sh
redis-cli -h "$FT_REDIS_MASTER_PORT_6379_TCP_ADDR" -p "$FT_REDIS_SERVICE_PORT" -a "$METALOAD_PASSWORD" --rdb "/backup/backup-redis/ftr-data.rdb"
# check if backup folder is empty
if [ -z "$(ls /backup/backup-redis)" ]; then
  echo "backup folder is empty"
  exit 1
fi
echo "finish copying dump file"
