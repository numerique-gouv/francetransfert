#!/bin/sh

now=$(date +"%Y_%m_%d_%H%M%S")
year=$(date +"%Y")
day=$(date +"%d")
month=$(date +"%m")

backup_folder="/backup"

echo "compress backup folder"
tar cfz $backup_folder/redis-dump.tar.gz $backup_folder
echo "upload redis dump to s3"
aws s3 cp $backup_folder/redis-dump.tar.gz s3://$BACKUP_BUCKET_NAME/redis-dump-${now}.tar.gz --endpoint-url $STORAGE_ENDPOINT
echo "finish backup"
