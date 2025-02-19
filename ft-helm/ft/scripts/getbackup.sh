#!/bin/sh

backup_folder="/backup"
rm -rf $backup_folder
mkdir -p $backup_folder/backup
echo "download redis dump from s3"
aws --no-verify-ssl s3 cp s3://$BACKUP_BUCKET_NAME/redis-dump-${BACKUP_DATE}.tar.gz $backup_folder/redis-dump.tar.gz --endpoint-url $STORAGE_ENDPOINT
echo "finish download"

echo "extract redis dump"

if [ -f "$backup_folder/redis-dump.tar.gz" ]; then
    tar -xzf $backup_folder/redis-dump.tar.gz -C $backup_folder/backup
    mv $backup_folder/backup/backup $backup_folder/backup/data
    echo "finish extract"
else
    echo "redis-dump.tar.gz not found"
    exit 1
fi