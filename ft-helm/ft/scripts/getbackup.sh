#!/bin/sh

backup_folder="/backup"
rm -rf $backup_folder
mkdir -p $backup_folder/backup
mkdir -p $backup_folder/data
echo "download redis dump from s3"
aws --no-verify-ssl s3 cp s3://$BACKUP_BUCKET_NAME/redis_backup_${BACKUP_DATE}.zip $backup_folder/redis-dump.zip --endpoint-url $STORAGE_ENDPOINT
echo "finish download"

echo "extract redis dump"

if [ -f "$backup_folder/redis-dump.zip" ]; then
    echo `ls -al $backup_folder/**`
    unzip $backup_folder/redis-dump.zip -d $backup_folder/data
    echo `ls -al $backup_folder/**`
    echo "finish extract"
else
    echo "redis-dump.zip not found"
    exit 1
fi