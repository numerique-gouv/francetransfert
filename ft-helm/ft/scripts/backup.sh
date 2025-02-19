#!/bin/sh

now=$(date +"%Y_%m_%d_%H%M%S")
year=$(date +"%Y")
day=$(date +"%d")
month=$(date +"%m")

backup_folder="/backup/backup-redis"

echo "compress backup folder"
cd $backup_folder
zip -r $backup_folder/redis-dump.zip .
echo "upload redis dump to s3"
aws --no-verify-ssl s3 cp $backup_folder/redis-dump.zip s3://$BACKUP_BUCKET_NAME/redis_backup_${now}.zip --endpoint-url $STORAGE_ENDPOINT
if [ $? -ne 0 ]; then
  echo "failed to upload redis dump to s3"
  exit 1
fi
delete_date=$(date -d @$(($(date +%s) - 86400 * 30)) +%Y-%m-%d)
echo "clean old redis dump in s3"
aws --no-verify-ssl s3api list-objects --endpoint-url $STORAGE_ENDPOINT --bucket $BACKUP_BUCKET_NAME --query 'Contents[?LastModified<=`'$delete_date'`].{Key:Key}' --output text | xargs -I {} aws s3 rm s3://$BACKUP_BUCKET_NAME/{} --no-verify-ssl --endpoint-url $STORAGE_ENDPOINT
echo "finish backup"