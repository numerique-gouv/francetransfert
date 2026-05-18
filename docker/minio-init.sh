#!/bin/sh
set -eu

# Wait for MinIO to answer, then create the buckets France Transfert needs.
#
# Layout matches what the Java code expects:
#   <BUCKET_PREFIX><enclosureId>  → per-pli bucket, created lazily by upload-api
#   <BUCKET_SEQUESTRE>            → single shared bucket, must exist beforehand
#
# We also pre-create one named bucket so `mc` can list and the admin paths
# don't 404 on empty deployments.

echo "Waiting for MinIO to be ready at ${STORAGE_ENDPOINT}..."
until mc alias set local "$STORAGE_ENDPOINT" "$STORAGE_ACCESS_KEY" "$STORAGE_SECRET_KEY" >/dev/null 2>&1 \
    && mc ready local >/dev/null 2>&1; do
  sleep 1
done

mc mb --ignore-existing "local/${BUCKET_SEQUESTRE}"
mc anonymous set download "local/${BUCKET_SEQUESTRE}" || true
echo "MinIO buckets ready."
