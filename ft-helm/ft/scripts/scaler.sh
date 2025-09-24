#!/bin/sh

scaler_min_replica_count=$SCALER_MIN_REPLICA_COUNT
scaler_max_replica_count=$SCALER_MAX_REPLICA_COUNT
worker_min_replica_count=$WORKER_MIN_REPLICA_COUNT
worker_max_replica_count=$WORKER_MAX_REPLICA_COUNT

echo "scaler_min_replica_count: $scaler_min_replica_count"
echo "scaler_max_replica_count: $scaler_max_replica_count"
echo "worker_min_replica_count: $worker_min_replica_count"
echo "worker_max_replica_count: $worker_max_replica_count"

# get scaler replica count
scaler_replica_count=$(kubectl get deployment $RELEASE_NAME-worker-scaler -o jsonpath='{.status.replicas}')
echo "scaler_replica_count: $scaler_replica_count"

# get worker replica count
worker_replica_count=$(kubectl get deployment $RELEASE_NAME-worker-app -o jsonpath='{.status.replicas}')
echo "worker_replica_count: $worker_replica_count"

echo "date: $(date)"
# if week day between 7:00 and 23:00 GMT+1
if [ $(date +%H) -ge 6 ] && [ $(date +%H) -lt 21 ] && [ $(date +%u) -ge 1 ] && [ $(date +%u) -le 5 ]; then
  echo "scale up worker and scale down scaler"
  kubectl scale deployment $RELEASE_NAME-worker-app --replicas=$worker_max_replica_count --timeout=300s
  sleep 180s
  kubectl scale deployment $RELEASE_NAME-worker-scaler --replicas=$scaler_min_replica_count --timeout=300s
  exit 0
fi

# if weekend or weekday between 23:00 and 7:00 GMT+1
if [ $(date +%H) -ge 21 ] || [ $(date +%H) -lt 6 ] || [ $(date +%u) -ge 6 ]; then
  echo "scale up scaler and scale down worker"
  kubectl scale deployment $RELEASE_NAME-worker-scaler --replicas=$scaler_max_replica_count --timeout=300s
  sleep 180s
  kubectl scale deployment $RELEASE_NAME-worker-app --replicas=$worker_min_replica_count --timeout=300s
  exit 0
fi