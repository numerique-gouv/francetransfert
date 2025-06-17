#!/bin/sh

backup_folder="/backup"
rm -rf $backup_folder/*
mkdir -p $backup_folder/backup
mkdir -p $backup_folder/data
echo "download redis dump from s3"
# aws configure set default.s3.max_concurrent_requests 20
# aws configure set default.s3.max_queue_size 10000
# aws configure set default.s3.multipart_threshold 64MB
# aws configure set default.s3.multipart_chunksize 16MB
aws --no-verify-ssl s3 cp s3://$BACKUP_BUCKET_NAME/redis_backup_${BACKUP_DATE}.zip $backup_folder/redis-dump.zip --endpoint-url $STORAGE_ENDPOINT
echo "finish download"

echo "extract redis dump"

if [ -f "$backup_folder/redis-dump.zip" ]; then
    unzip $backup_folder/redis-dump.zip -d $backup_folder/data
    echo "finish extract"
else
    echo "redis-dump.zip not found"
    exit 1
fi